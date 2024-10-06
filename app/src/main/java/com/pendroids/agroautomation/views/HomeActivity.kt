package com.pendroids.agroautomation.views

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.pendroids.agroautomation.R
import com.pendroids.agroautomation.databinding.ActivityHomeBinding
import com.pendroids.agroautomation.fragments.AlertFragment
import com.pendroids.agroautomation.fragments.DashboardFragment

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        replaceFragment(DashboardFragment())
        fragmentSwitcher();
    }
    private fun fragmentSwitcher() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.dashboard -> replaceFragment(DashboardFragment())
                R.id.alerts -> replaceFragment(AlertFragment())
                else -> {

                }
            }
            true
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        val fragmentManger = supportFragmentManager
        val fragmentTransaction = fragmentManger.beginTransaction()
        fragmentTransaction.replace(R.id.fragmentContainer, fragment)
        fragmentTransaction.commit()
    }
}