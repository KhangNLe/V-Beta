package edu.ics499.VBeta.application.support;

import java.net.URL;

public interface VideoStoragePort {
    URL generateSignedPutURL(String objectName, String contentType);
    String generatePublicURL(String bucketName, String fileName);
}
