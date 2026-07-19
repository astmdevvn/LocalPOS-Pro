package com.localpos.pro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.localpos.pro.data.PosRepository
import com.localpos.pro.ui.LocalPosApp
import com.localpos.pro.ui.PosViewModel
import com.localpos.pro.ui.theme.LocalPosTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repository = (application as LocalPosApplication).repository
        setContent {
            LocalPosTheme {
                val vm: PosViewModel = viewModel(factory = PosViewModelFactory(repository))
                LocalPosApp(vm)
            }
        }
    }
}

private class PosViewModelFactory(private val repository: PosRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = PosViewModel(repository) as T
}
