const fs = require("fs");
const path = require("path");
const admin = require("firebase-admin");

const COMMIT = process.argv.includes("--commit");
const COLLECTION = process.env.PLACES_COLLECTION || "places";
const BATCH_SIZE = 450;

function readProjectId() {
  if (process.env.GOOGLE_CLOUD_PROJECT) return process.env.GOOGLE_CLOUD_PROJECT;
  if (process.env.GCLOUD_PROJECT) return process.env.GCLOUD_PROJECT;

  const googleServicesPath = path.resolve(__dirname, "../../app/google-services.json");
  const googleServices = JSON.parse(fs.readFileSync(googleServicesPath, "utf8"));
  return googleServices.project_info.project_id;
}

function shouldResetRatingAvg(data) {
  if (!Object.prototype.hasOwnProperty.call(data, "ratingAvg")) return false;
  return Number(data.ratingAvg) !== 0;
}

async function main() {
  const projectId = readProjectId();
  admin.initializeApp({ projectId });

  const db = admin.firestore();
  const snapshot = await db.collection(COLLECTION).where("totalRatings", "==", 0).get();
  const staleDocs = snapshot.docs.filter((doc) => shouldResetRatingAvg(doc.data()));

  console.log(`Project: ${projectId}`);
  console.log(`Collection: ${COLLECTION}`);
  console.log(`Matched totalRatings == 0: ${snapshot.size}`);
  console.log(`Stale ratingAvg docs: ${staleDocs.length}`);

  if (!COMMIT) {
    staleDocs.slice(0, 20).forEach((doc) => {
      console.log(`DRY RUN ${doc.ref.path}: ratingAvg=${doc.get("ratingAvg")}`);
    });
    if (staleDocs.length > 20) {
      console.log(`DRY RUN omitted ${staleDocs.length - 20} more docs.`);
    }
    console.log("No changes written. Re-run with --commit to apply ratingAvg=0.");
    return;
  }

  let updated = 0;
  for (let i = 0; i < staleDocs.length; i += BATCH_SIZE) {
    const batch = db.batch();
    const docs = staleDocs.slice(i, i + BATCH_SIZE);
    docs.forEach((doc) => batch.update(doc.ref, { ratingAvg: 0 }));
    await batch.commit();
    updated += docs.length;
  }

  console.log(`Updated ${updated} place docs.`);
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
