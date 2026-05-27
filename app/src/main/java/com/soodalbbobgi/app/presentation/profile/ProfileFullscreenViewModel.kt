package com.soodalbbobgi.app.presentation.profile

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

/** 갤러리 저장/공유 작업 상태 */
sealed interface SaveState {
    data object Idle : SaveState
    data object Saving : SaveState
    data object Success : SaveState
    data class Error(val message: String) : SaveState
}

/**
 * 프로필 카드 전체보기 화면의 저장/공유 상태를 관리한다.
 */
@HiltViewModel
class ProfileFullscreenViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _saveState = MutableStateFlow<SaveState>(SaveState.Idle)
    val saveState: StateFlow<SaveState> = _saveState

    /** 상태를 초기화한다. Success/Error 토스트 표시 후 호출. */
    fun resetState() {
        _saveState.value = SaveState.Idle
    }

    /**
     * 카드 Bitmap을 갤러리에 PNG로 저장한다.
     * Android 10+ MediaStore API 사용, 별도 권한 불필요.
     *
     * @param bitmap 저장할 프로필 카드 Bitmap
     */
    fun saveToGallery(bitmap: Bitmap) {
        viewModelScope.launch(Dispatchers.IO) {
            _saveState.value = SaveState.Saving
            try {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, "soodal_card_${System.currentTimeMillis()}.png")
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/SoodalBbobgi")
                }
                val uri = context.contentResolver.insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values
                )
                uri?.let {
                    context.contentResolver.openOutputStream(it)?.use { os ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, os)
                    }
                } ?: throw IllegalStateException("MediaStore URI 생성 실패")
                _saveState.value = SaveState.Success
            } catch (e: Exception) {
                _saveState.value = SaveState.Error(e.message ?: "저장 실패")
            }
        }
    }

    /**
     * 카드 Bitmap을 임시 파일로 저장 후 Android ShareSheet를 실행한다.
     *
     * @param bitmap 공유할 프로필 카드 Bitmap
     */
    fun share(bitmap: Bitmap) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val cacheDir = File(context.cacheDir, "shared_cards").apply { mkdirs() }
                val file = File(cacheDir, "soodal_card.png")
                FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                val chooser = Intent.createChooser(intent, "프로필 카드 공유").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(chooser)
            } catch (e: Exception) {
                _saveState.value = SaveState.Error(e.message ?: "공유 실패")
            }
        }
    }
}
