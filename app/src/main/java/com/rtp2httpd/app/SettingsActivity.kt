package com.rtp2httpd.app

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.rtp2httpd.app.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = getString(R.string.settings_title)

        binding.etServer.setText(SettingsManager.getServerUrl(this) ?: "")

        binding.btnSave.setOnClickListener {
            val raw = binding.etServer.text?.toString()?.trim().orEmpty()
            if (!SettingsManager.isValid(raw)) {
                Toast.makeText(this, R.string.invalid_url, Toast.LENGTH_SHORT).show()
                binding.tilServer.error = getString(R.string.invalid_url)
                return@setOnClickListener
            }
            binding.tilServer.error = null
            SettingsManager.setServerUrl(this, raw)
            Toast.makeText(this, "已保存", Toast.LENGTH_SHORT).show()
            setResult(RESULT_OK)
            finish()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
