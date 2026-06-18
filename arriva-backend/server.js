require("dotenv").config();

const express = require("express");
const rateLimit = require("express-rate-limit");

const app = express();
const PORT = process.env.PORT || 3000;
const ANTHROPIC_API_URL = "https://api.anthropic.com/v1/messages";
const ANTHROPIC_MODEL = process.env.ANTHROPIC_MODEL || "claude-sonnet-4-6";
const ITINERARY_SYSTEM_PROMPT = "You are a travel guide assistant. Always respond with valid JSON only.";
const ARRIVA_SYSTEM_PROMPT =
  "You are Arriva, a knowledgeable and friendly Maharashtra travel companion. "
  + "Your purpose is to help travelers explore Maharashtra — its forts, temples, nature spots, street food, "
  + "cultural events, and hidden gems. Answer only tourism and travel-related questions about Maharashtra and India. "
  + "Refuse politely but firmly if asked about unrelated topics. Keep answers concise and practical.";
const MAX_CONTEXT_PLACES = 30;
const MAX_REQUEST_PLACES = 60;

app.set("trust proxy", 1);
app.use(express.json({ limit: process.env.REQUEST_BODY_LIMIT || "64kb" }));

// ─── Shared auth middleware ────────────────────────────────────────────────────
// Every /api/* route requires the X-App-Token header to match the server env.
function requireAppToken(req, res, next) {
  const expectedToken = process.env.APP_TOKEN;
  if (!expectedToken) {
    // If the server operator hasn't set a token, allow through (dev mode).
    return next();
  }
  const provided = req.headers["x-app-token"];
  if (!provided || provided !== expectedToken) {
    return res.status(403).json({ error: "Forbidden: invalid or missing X-App-Token." });
  }
  return next();
}

const chatLimiter = rateLimit({
  windowMs: 60 * 1000,          // 1-minute window
  limit: 30,                     // 30 messages per user per minute
  standardHeaders: "draft-7",
  legacyHeaders: false,
  message: { error: "Too many chat requests. Please slow down." }
});

const itineraryLimiter = rateLimit({
  windowMs: 60 * 60 * 1000,
  limit: 10,
  standardHeaders: "draft-7",
  legacyHeaders: false,
  message: { error: "Too many itinerary requests. Please try again later." }
});

// ─── POST /api/chat ────────────────────────────────────────────────────────────
// Body: { messages: [{role, content}], systemPrompt?: string }
// Returns: { content: string }
app.post("/api/chat", requireAppToken, chatLimiter, async (req, res) => {
  try {
    const apiKey = process.env.ANTHROPIC_API_KEY;
    if (!apiKey) {
      return res.status(500).json({ error: "Chat service is not configured." });
    }

    const { messages, systemPrompt } = req.body || {};
    if (!Array.isArray(messages) || messages.length === 0) {
      return res.status(400).json({ error: "messages must be a non-empty array." });
    }
    if (messages.length > 200) {
      return res.status(413).json({ error: "Conversation is too long (max 200 messages)." });
    }

    // Validate and sanitize each message
    const sanitized = [];
    for (const msg of messages) {
      const role = String(msg.role || "").toLowerCase();
      const content = String(msg.content || "").trim();
      if ((role !== "user" && role !== "assistant") || !content) continue;
      sanitized.push({ role, content });
    }
    if (sanitized.length === 0) {
      return res.status(400).json({ error: "No valid messages found." });
    }

    const effectiveSystem = (typeof systemPrompt === "string" && systemPrompt.trim())
      ? systemPrompt.trim()
      : ARRIVA_SYSTEM_PROMPT;

    const response = await callAnthropicChat(apiKey, sanitized, effectiveSystem);
    const text = extractAssistantText(response);
    return res.json({ content: text });
  } catch (error) {
    const status = Number.isInteger(error.status) ? error.status : 500;
    return res.status(status).json({ error: error.publicMessage || "Chat request failed." });
  }
});

// ─── POST /api/plan-trip ───────────────────────────────────────────────────────
// Body: { duration: number, vibes: string[], budget: string, city: string }
// Returns: { tripTitle, days, generalTips }
app.post("/api/plan-trip", requireAppToken, itineraryLimiter, async (req, res) => {
  try {
    const apiKey = process.env.ANTHROPIC_API_KEY;
    if (!apiKey) {
      return res.status(500).json({ error: "Trip planner service is not configured." });
    }

    const body = req.body || {};
    const duration = Math.max(1, Math.min(7, Number(body.duration) || 1));
    const vibes = Array.isArray(body.vibes) ? body.vibes.map(String) : ["Mixed"];
    const budget = String(body.budget || "Mid-range").trim();
    const city = String(body.city || "Pune").trim();

    const prompt = buildPlanTripPrompt(duration, vibes, budget, city);
    const response = await callAnthropicChat(apiKey, [{ role: "user", content: prompt }],
      "You are a travel expert for Maharashtra, India. Always respond with valid JSON only.");
    const text = extractAssistantText(response);
    const plan = parseAssistantJson(text);
    return res.json(plan);
  } catch (error) {
    const status = Number.isInteger(error.status) ? error.status : 500;
    return res.status(status).json({ error: error.publicMessage || "Could not generate trip plan." });
  }
});

// ─── POST /api/generate-itinerary ─────────────────────────────────────────────
// (Existing itinerary endpoint — kept for backward compatibility)
app.post("/api/generate-itinerary", requireAppToken, itineraryLimiter, async (req, res) => {
  try {
    const apiKey = process.env.ANTHROPIC_API_KEY;
    if (!apiKey) {
      return res.status(500).json({ error: "AI itinerary service is not configured." });
    }

    const payload = validatePayload(req.body);
    const prompt = buildAiPrompt(payload.days, payload.type, payload.budget, payload.places);
    const anthropicResponse = await callAnthropicChat(
      apiKey,
      [{ role: "user", content: prompt }],
      ITINERARY_SYSTEM_PROMPT
    );
    const assistantText = extractAssistantText(anthropicResponse);
    const itinerary = parseAssistantJson(assistantText);

    return res.json(itinerary);
  } catch (error) {
    const status = Number.isInteger(error.status) ? error.status : 500;
    return res.status(status).json({ error: error.publicMessage || "Could not generate itinerary." });
  }
});

app.use((error, req, res, next) => {
  if (error && error.type === "entity.too.large") {
    return res.status(413).json({ error: "Request body is too large." });
  }
  if (error && error.type === "entity.parse.failed") {
    return res.status(400).json({ error: "Request body must be valid JSON." });
  }
  return next(error);
});

function validatePayload(body) {
  const days = Number(body && body.days);
  const places = body && body.places;

  if (!Number.isInteger(days) || days < 1 || days > 7) {
    throw publicError(400, "days must be an integer from 1 to 7.");
  }

  if (!Array.isArray(places)) {
    throw publicError(400, "places must be an array.");
  }

  if (places.length === 0) {
    throw publicError(400, "places must include at least one place.");
  }

  if (places.length > MAX_REQUEST_PLACES) {
    throw publicError(413, `places is capped at ${MAX_REQUEST_PLACES} items.`);
  }

  return {
    days,
    type: valueOrDefault(body.type, "Mixed"),
    budget: valueOrDefault(body.budget, "Mid-range"),
    places: places.map(normalizePlace)
  };
}

function buildAiPrompt(days, type, budget, places) {
  const contextString = buildPlacesContext(type, budget, places);
  const tripDescription = type && type.includes(',')
    ? "The user wants a " + days + "-day trip featuring a mix of: " + type + " spots, on a " + budget + " budget."
    : "The user wants a " + days + "-day " + type + " trip on a " + budget + " budget.";
  return "You are a travel expert for Pune, India. " + tripDescription + "\n"
    + "Available places:\n" + contextString + "\n\n"
    + "Return ONLY a valid JSON object (no extra text, no markdown) in this exact format:\n"
    + "{\n"
    + "  \"tripTitle\": \"Your 2-Day Nature Escape in Pune\",\n"
    + "  \"days\": [\n"
    + "    {\n"
    + "      \"dayNumber\": 1,\n"
    + "      \"dayTheme\": \"Morning freshness and green trails\",\n"
    + "      \"stops\": [\n"
    + "        {\n"
    + "          \"placeName\": \"Sinhagad Fort\",\n"
    + "          \"timeSlot\": \"Morning (8am-11am)\",\n"
    + "          \"duration\": \"3 hours\",\n"
    + "          \"travelNote\": \"Start here for the day.\",\n"
    + "          \"whyVisit\": \"Best experienced at sunrise before crowds arrive.\",\n"
    + "          \"tips\": \"Carry water and wear comfortable shoes.\"\n"
    + "        }\n"
    + "      ]\n"
    + "    }\n"
    + "  ],\n"
    + "  \"generalTips\": \"Book accommodation near Koregaon Park for easy access.\"\n"
    + "}\n\n"
    + "Rules:\n"
    + "- Only use places from the provided list.\n"
    + "- Do not repeat a place on the same day.\n"
    + "- Spread stops sensibly across morning/afternoon/evening.\n"
    + "- Minimize travel between consecutive stops on the same day — prefer geographically clustered places for each day's itinerary.\n"
    + "- Match each place's best visiting time (morning for outdoor/nature/forts, midday for indoor/museums, evening for markets/sunset spots/riverfronts) to the appropriate time slot.\n"
    + "- Include a travelNote for every stop. For the first stop of each day, use a brief start note; for later stops, estimate travel distance or time from the previous stop.\n"
    + "- Match the type filter: if type is 'Nature', only include nature places.\n"
    + "- If type is 'Mixed', use variety across categories.\n"
    + "- If multiple types are given (comma-separated), include places from ALL listed categories, not just one.\n"
    + "- Respect the budget: for 'Budget' trips, avoid places with budget='High'";
}

function buildPlacesContext(type, budget, places) {
  let contextPlaces;
  if (type && type.includes(',')) {
    const types = type.split(',').map(t => t.trim());
    contextPlaces = places.filter(place => types.some(t => matchesType(place, t)) && matchesBudget(place, budget));
  } else {
    contextPlaces = places.filter(place => matchesType(place, type) && matchesBudget(place, budget));
  }
  if (contextPlaces.length === 0) {
    contextPlaces = [...places];
  }

  contextPlaces.sort((a, b) => b.rating - a.rating);

  return contextPlaces.slice(0, MAX_CONTEXT_PLACES).map((place) => {
    const category = valueOrDefault(place.category, "Mixed");
    const rating = place.totalRatings > 0
      ? `${place.rating.toFixed(1)} (${place.totalRatings} reviews)`
      : "New (0 reviews)";
    const bestFor = valueOrDefault(place.tag, `${category} lovers`);
    const placeBudget = valueOrDefault(place.budget, "Medium");
    const bestTime = valueOrDefault(place.bestTime, "Any");
    const coordinates = hasValidCoordinates(place)
      ? `${place.latitude.toFixed(6)}, ${place.longitude.toFixed(6)}`
      : "Unknown";
    const description = trimToLength(valueOrDefault(place.description, "No description available."), 140);

    return "Place: " + valueOrDefault(place.name, "Unnamed place")
      + " | Category: " + category
      + " | Rating: " + rating
      + " | Best for: " + bestFor
      + " | Best time: " + bestTime
      + " | Budget: " + placeBudget
      + " | City: " + valueOrDefault(place.city, "Pune")
      + " | Coordinates: " + coordinates
      + " | Description: " + description;
  }).join("\n");
}

// Generic Anthropic messages call. `messages` is [{role, content}], `system` is a string.
async function callAnthropicChat(apiKey, messages, system) {
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), 45000);

  try {
    const response = await fetch(ANTHROPIC_API_URL, {
      method: "POST",
      headers: {
        "content-type": "application/json",
        "x-api-key": apiKey,
        "anthropic-version": "2023-06-01"
      },
      body: JSON.stringify({
        model: ANTHROPIC_MODEL,
        max_tokens: 2048,
        system: system || "",
        messages
      }),
      signal: controller.signal
    });

    if (!response.ok) {
      const errBody = await response.json().catch(() => ({}));
      const msg = errBody && errBody.error && errBody.error.message
        ? errBody.error.message
        : "AI service returned an error.";
      throw publicError(502, msg);
    }

    return await response.json();
  } catch (error) {
    if (error.name === "AbortError") {
      throw publicError(504, "AI service timed out.");
    }
    if (error.publicMessage) throw error;
    throw publicError(502, "Could not reach AI service.");
  } finally {
    clearTimeout(timeout);
  }
}

// ─── POST /api/plan-trip prompt builder ──────────────────────────────────────
function buildPlanTripPrompt(duration, vibes, budget, city) {
  const vibesStr = vibes.join(", ") || "Mixed";
  return "Plan a " + duration + "-day trip to " + city + " with vibes: " + vibesStr
    + " on a " + budget + " budget.\n\n"
    + "Return ONLY a valid JSON object (no markdown, no extra text) in this format:\n"
    + "{\n"
    + "  \"tripTitle\": \"Your 2-Day Cultural Escape in Pune\",\n"
    + "  \"days\": [\n"
    + "    {\n"
    + "      \"dayNumber\": 1,\n"
    + "      \"dayTheme\": \"Historical forts and temples\",\n"
    + "      \"stops\": [\n"
    + "        {\n"
    + "          \"placeName\": \"Shaniwar Wada\",\n"
    + "          \"timeSlot\": \"Morning (8am–11am)\",\n"
    + "          \"duration\": \"2 hours\",\n"
    + "          \"travelNote\": \"Starting point for Day 1.\",\n"
    + "          \"whyVisit\": \"Iconic Maratha fort in the heart of Pune.\",\n"
    + "          \"tips\": \"Visit the sound and light show in the evening.\"\n"
    + "        }\n"
    + "      ]\n"
    + "    }\n"
    + "  ],\n"
    + "  \"generalTips\": \"Carry water and wear comfortable shoes.\"\n"
    + "}";
}

function extractAssistantText(response) {
  if (!response || !Array.isArray(response.content)) {
    throw publicError(502, "AI itinerary service returned an empty response.");
  }

  const text = response.content
    .filter((block) => block && block.type === "text" && typeof block.text === "string")
    .map((block) => block.text.trim())
    .filter(Boolean)
    .join("\n");

  if (!text) {
    throw publicError(502, "AI itinerary service returned an empty response.");
  }

  return text;
}

function parseAssistantJson(text) {
  const jsonText = extractJsonObject(text);
  try {
    const itinerary = JSON.parse(jsonText);
    if (!Array.isArray(itinerary.days)) {
      throw new Error("Missing days array.");
    }
    return itinerary;
  } catch (error) {
    throw publicError(502, "AI itinerary service returned invalid itinerary JSON.");
  }
}

function extractJsonObject(text) {
  let cleaned = text.trim();
  if (cleaned.startsWith("```")) {
    cleaned = cleaned.replace(/^```json/i, "").replace(/^```/, "").replace(/```$/, "").trim();
  }

  const start = cleaned.indexOf("{");
  const end = cleaned.lastIndexOf("}");
  if (start < 0 || end <= start) {
    throw publicError(502, "AI itinerary service did not return a JSON object.");
  }
  return cleaned.substring(start, end + 1);
}

function normalizePlace(place) {
  return {
    name: valueOrDefault(place && place.name, "Unnamed place"),
    category: valueOrDefault(place && place.category, "Mixed"),
    rating: numberOrDefault(place && place.rating, 0),
    totalRatings: Math.max(0, Math.trunc(numberOrDefault(place && place.totalRatings, 0))),
    tag: valueOrDefault(place && place.tag, ""),
    budget: valueOrDefault(place && place.budget, "Medium"),
    bestTime: valueOrDefault(place && place.bestTime, "Any"),
    city: valueOrDefault(place && place.city, "Pune"),
    latitude: numberOrDefault(place && place.latitude, null),
    longitude: numberOrDefault(place && place.longitude, null),
    description: trimToLength(valueOrDefault(place && place.description, "No description available."), 280)
  };
}

function hasValidCoordinates(place) {
  return Number.isFinite(place.latitude)
    && Number.isFinite(place.longitude)
    && place.latitude >= -90
    && place.latitude <= 90
    && place.longitude >= -180
    && place.longitude <= 180
    && !(Math.abs(place.latitude) < 0.000001 && Math.abs(place.longitude) < 0.000001);
}

function matchesType(place, type) {
  if (isBlank(type) || equalsIgnoreCase(type, "Mixed") || equalsIgnoreCase(type, "All")) {
    return true;
  }
  return equalsIgnoreCase(place.category, type);
}

function matchesBudget(place, budget) {
  if (isBlank(budget) || equalsIgnoreCase(budget, "Luxury")) {
    return true;
  }

  if (isBlank(place.budget)) {
    return true;
  }

  if (equalsIgnoreCase(budget, "Budget") || equalsIgnoreCase(budget, "Mid-range")) {
    return !equalsIgnoreCase(place.budget, "High") && !equalsIgnoreCase(place.budget, "Luxury");
  }

  return true;
}

function trimToLength(value, maxLength) {
  const trimmed = String(value || "").trim().replace(/\s+/g, " ");
  if (trimmed.length <= maxLength) return trimmed;
  return trimmed.substring(0, Math.max(0, maxLength - 3)).trim() + "...";
}

function valueOrDefault(value, fallback) {
  return isBlank(value) ? fallback : String(value).trim();
}

function numberOrDefault(value, fallback) {
  const number = Number(value);
  return Number.isFinite(number) ? number : fallback;
}

function isBlank(value) {
  return value == null || String(value).trim().length === 0;
}

function equalsIgnoreCase(left, right) {
  return String(left || "").toLowerCase() === String(right || "").toLowerCase();
}

function publicError(status, publicMessage) {
  const error = new Error(publicMessage);
  error.status = status;
  error.publicMessage = publicMessage;
  return error;
}

app.listen(PORT, () => {
  console.log(`Arriva backend listening on port ${PORT}`);
});
