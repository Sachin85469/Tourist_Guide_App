package com.arriva.touristguideapp.communication.translation;

/**
 * Download / readiness state for an ML Kit translation model (per language code).
 */
public enum TranslationModelState {
    NOT_DOWNLOADED,
    DOWNLOADING,
    READY,
    FAILED
}
