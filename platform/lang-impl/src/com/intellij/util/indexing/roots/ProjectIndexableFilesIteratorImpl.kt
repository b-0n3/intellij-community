// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing.roots

import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ContentIterator
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileFilter
import com.intellij.util.indexing.IndexingBundle
import com.intellij.util.indexing.roots.kind.ProjectFileOrDirOrigin
import com.intellij.util.indexing.roots.kind.ProjectFileOrDirOriginImpl

class ProjectIndexableFilesIteratorImpl(private val fileOrDir: VirtualFile) : ProjectIndexableFilesIterator {
  override fun getDebugName(): String = "Files under `${fileOrDir.path}`"

  override fun getIndexingProgressText(): String {
    return IndexingBundle.message("indexable.files.provider.indexing.fileOrDir.name", fileOrDir.name)
  }

  override fun getRootsScanningProgressText(): String {
    return IndexingBundle.message("indexable.files.provider.scanning.fileOrDir.name", fileOrDir.name)
  }

  override fun getOrigin(): ProjectFileOrDirOrigin = ProjectFileOrDirOriginImpl(fileOrDir)

  override fun iterateFiles(
    project: Project,
    fileIterator: ContentIterator,
    fileFilter: VirtualFileFilter
  ): Boolean = ProjectFileIndex.getInstance(project).iterateContentUnderDirectory(fileOrDir, fileIterator, fileFilter)

  override fun getRootUrls(): Set<String> {
    throw UnsupportedOperationException()
  }
}