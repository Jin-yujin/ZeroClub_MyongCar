package com.example.mju_car

import android.app.Application
import com.example.mju_car.data.AuthRepository
import com.example.mju_car.data.AuthService
import com.example.mju_car.data.StorageRepository
import com.example.mju_car.data.StorageService
import com.example.mju_car.ui.register.RegisterViewModel
import com.google.firebase.FirebaseApp
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.dsl.module

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        // Firebase 초기화
        FirebaseApp.initializeApp(this)
        // Koin 초기화
        startKoin {
            androidContext(this@App) // 애플리케이션 컨텍스트를 Koin에 설정
            modules(singleModule, viewModelModule) // 모듈 등록
        }
    }
}

// Koin 모듈 설정
val singleModule = module {
    single<AuthService> { AuthRepository(androidContext()) }
    single<StorageService> { StorageRepository() }
}

// ViewModel 모듈 설정
val viewModelModule = module {
    viewModel { RegisterViewModel() }
}