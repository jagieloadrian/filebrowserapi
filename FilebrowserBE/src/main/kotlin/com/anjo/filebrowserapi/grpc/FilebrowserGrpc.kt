package com.anjo.filebrowserapi.grpc

import com.anjo.filebrowser.Filebrowser
import com.anjo.filebrowser.Filebrowser.FileResponse
import com.anjo.filebrowser.Filebrowser.StringResponse
import com.anjo.filebrowser.Filebrowser.StringsResponse
import com.anjo.filebrowser.FilebrowserApiGrpcKt.FilebrowserApiCoroutineImplBase
import com.anjo.filebrowserapi.service.DirectoryManager
import com.anjo.filebrowserapi.service.FileManager
import com.anjo.filebrowserapi.utils.ByteArrayToMultiPartFile
import com.google.protobuf.Empty
import com.google.protobuf.kotlin.toByteString
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile

@Profile("server")
@Service
class FilebrowserGrpc(
    private val directoryManager: DirectoryManager,
    private val fileManager: FileManager,
) : FilebrowserApiCoroutineImplBase() {
    override suspend fun showFilesTree(request: Filebrowser.PathRequest): StringsResponse {
        val fileTree = fileManager.showFilesTree(request.path)
        return StringsResponse
            .newBuilder()
            .addAllDirectories(fileTree)
            .build()
    }

    override suspend fun uploadFile(request: Filebrowser.UploadFile): StringResponse {
        val uploadFileLocation =
            with(getMultipartFile(request)) {
                fileManager.uploadFile(this, request.path, request.overwrite)
            }
        return StringResponse
            .newBuilder()
            .setPath(uploadFileLocation)
            .build()
    }

    override suspend fun getFile(request: Filebrowser.FileOperation): FileResponse {
        val file = fileManager.getFile(request.name, request.path)
        return FileResponse
            .newBuilder()
            .setData(file.toByteString())
            .build()
    }

    override suspend fun renameFile(request: Filebrowser.FileOperation): StringResponse {
        val filePath = fileManager.renameFile(request.name, request.path)
        return StringResponse
            .newBuilder()
            .setPath(filePath)
            .build()
    }

    override suspend fun deleteFile(request: Filebrowser.FileOperation): Empty {
        fileManager.deleteFile(request.name, request.path)
        return Empty
            .newBuilder()
            .build()
    }

    override suspend fun showDirectoriesTree(request: Filebrowser.PathRequest): StringsResponse {
        val allDirectories = directoryManager.showDirectoriesTree(request.path)
        return StringsResponse
            .newBuilder()
            .addAllDirectories(allDirectories)
            .build()
    }

    override suspend fun addDirectory(request: Filebrowser.FileOperation): StringResponse {
        val directoryPath = directoryManager.addDirectory(request.name, request.path)
        return StringResponse
            .newBuilder()
            .setPath(directoryPath)
            .build()
    }

    override suspend fun addDirectoriesRecursively(request: Filebrowser.PathRequest): StringResponse {
        val directoryPath = directoryManager.addDirectoriesRecursively(request.path)
        return StringResponse
            .newBuilder()
            .setPath(directoryPath)
            .build()
    }

    override suspend fun renameDirectory(request: Filebrowser.FileOperation): StringResponse {
        val directoryPath = directoryManager.renameDirectory(request.name, request.path)
        return StringResponse
            .newBuilder()
            .setPath(directoryPath)
            .build()
    }

    override suspend fun deleteDirectory(request: Filebrowser.PathRequest): Empty {
        directoryManager.deleteDirectory(request.path)
        return Empty
            .newBuilder()
            .build()
    }

    private fun getMultipartFile(request: Filebrowser.UploadFile): MultipartFile =
        ByteArrayToMultiPartFile(request.data.toByteArray(), request.name, request.contentType)
}
