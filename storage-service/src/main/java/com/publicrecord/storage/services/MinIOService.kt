package com.publicrecord.storage.services

import io.minio.MinioClient
import io.minio.UploadObjectArgs
import io.minio.BucketExistsArgs
import io.minio.MakeBucketArgs
import io.minio.GetObjectArgs
import java.io.InputStream

class MinIOService {
    // Read configuration from environment variables
    private val endpoint: String = System.getenv("MINIO_ENDPOINT") ?: "http://minio:9000"
    private val accessKey: String = System.getenv("MINIO_ACCESS_KEY") ?: "minioadmin"
    private val secretKey: String = System.getenv("MINIO_SECRET_KEY") ?: "minioadmin"
    private val bucketName: String = System.getenv("MINIO_BUCKET") ?: "political-media"

    private val client = MinioClient.builder()
        .endpoint(endpoint)
        .credentials(accessKey, secretKey)
        .build()

    init {
        ensureBucketExists()
    }

    fun startService() {
        println("✅ MinIO Service Started. endpoint=$endpoint, bucket=$bucketName")
    }

    private fun ensureBucketExists() {
        try {
            if (!client.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build())) {
                client.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build())
            }
        } catch (e: Exception) {
            println("⚠️ Could not ensure MinIO bucket exists: ${e.message}")
        }
    }

    fun uploadFile(filePath: String, objectName: String) {
        client.uploadObject(
            UploadObjectArgs.builder()
                .bucket(bucketName)
                .`object`(objectName)
                .filename(filePath)
                .build()
        )
        println("File uploaded: $objectName")
    }

    fun getFile(objectName: String): InputStream {
        return client.getObject(
            GetObjectArgs.builder()
                .bucket(bucketName)
                .`object`(objectName)
                .build()
        )
    }
}
