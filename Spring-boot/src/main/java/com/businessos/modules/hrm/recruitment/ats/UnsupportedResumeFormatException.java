package com.businessos.modules.hrm.recruitment.ats;

/** Thrown by CvTextExtractor for a file extension it can't extract text from (e.g. legacy .doc). */
public class UnsupportedResumeFormatException extends Exception {
    public UnsupportedResumeFormatException(String extension) {
        super("Unsupported resume format: " + extension);
    }
}
