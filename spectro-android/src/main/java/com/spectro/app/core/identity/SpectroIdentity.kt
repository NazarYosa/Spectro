package com.spectro.app.core.identity

import android.content.Context
import com.spectro.app.R

/**
 * Identitas Resmi dan Atribusi Pengembang Spectro.
 * 
 * Sesuai dengan lisensi GNU General Public License v3.0 (GPL-3.0) Section 7(b):
 * "Requiring preservation of specified reasonable legal notices or author attributions
 * in that material or in the Appropriate Legal Notices displayed by works containing it."
 * 
 * Pengembang asli: Nazar Yosa
 * Repositori resmi: https://github.com/NazarYosa/Spectro
 * Setiap modifikasi, salinan, atau distribusi turunan dari proyek ini WAJIB mencantumkan
 * nama pengembang asli dan tautan repositori resmi ini tanpa pengecualian.
 */
object SpectroIdentity {
    const val AUTHOR_NAME = "Nazar Yosa"
    const val GITHUB_AUTHOR_URL = "https://github.com/NazarYosa"
    const val GITHUB_REPO_URL = "https://github.com/NazarYosa/Spectro"
    const val LICENSE_NAME = "GNU General Public License v3.0"
    const val LICENSE_URL = "https://github.com/NazarYosa/Spectro/blob/main/LICENSE"

    fun author(): String = AUTHOR_NAME

    fun repository(): String = GITHUB_REPO_URL

    fun license(): String = LICENSE_NAME

    fun verifyIntegrity(context: Context): Boolean {
        val declaredAuthor = context.getString(R.string.settings_about_author_name)
        val declaredRepo = context.getString(R.string.settings_about_source_address)
        return declaredAuthor == AUTHOR_NAME && declaredRepo == GITHUB_REPO_URL
    }
}
