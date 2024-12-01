package com.hardik.calendarapp.presentation.ui

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import com.hardik.calendarapp.R
import com.hardik.calendarapp.common.Constants.BASE_TAG
import com.hardik.calendarapp.databinding.ActivityMainBinding
import com.hardik.calendarapp.presentation.MainViewModel
import com.hardik.calendarapp.utillities.createYearData
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private final val TAG = BASE_TAG + MainActivity::class.java.simpleName

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    val mainViewModel: MainViewModel by viewModels()//by activityViewModels()
    lateinit var toolbar: Toolbar
    companion object{
        val yearList: Map<Int, Map<Int, List<Int>>> = createYearData(2000,2100, isZeroBased = true)
        val yearMonthPairList: List<Pair<Int, Int>> = yearList.flatMap { (year, monthsMap) -> monthsMap.keys.map { month -> year to month } }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        toolbar = binding.appBarMain.toolbar
        setSupportActionBar(toolbar)

        binding.appBarMain.fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null)
                .setAnchorView(R.id.fab).show()
        }
        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(
            setOf(R.id.nav_home, R.id.calendarMonthFragment, R.id.calendarMonth1Fragment,R.id.calendarYearFragment, R.id.calendarYear1Fragment
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

//        binding.appBarMain.toolbar.navigationIcon = null

        // Collecting the StateFlow
        lifecycleScope.launch {
            mainViewModel.holidayApiState.collect { dataState ->
                if (dataState.isLoading) {
                    // Show loading indicator
                    Log.d(TAG, "onCreate: Progressing")
                } else if (dataState.error.isNotEmpty()) {
                    // Show error message
                    Toast.makeText(this@MainActivity, dataState.error, Toast.LENGTH_SHORT).show()
                    Log.d(TAG, "onCreate: hide Progressing")
                } else {
                    // Update UI with the user list
                    val users = dataState.data
                    Log.d(TAG, "onCreate: hide Progressing")
//                 userAdapter.differ.submitList(users.toList())
//                 binding.recyclerview.setPadding(0, 0, 0, 0)
                }
            }
        }
        navController.addOnDestinationChangedListener { _, destination, _ -> invalidateOptionsMenu() }
    }

//    override fun onCreateOptionsMenu(menu: Menu): Boolean {
//        // Inflate the menu; this adds items to the action bar if it is present.
//        menuInflater.inflate(R.menu.main, menu)
//        return true
//    }
//
//    override fun onOptionsItemSelected(item: MenuItem): Boolean {
//        val navController = findNavController(R.id.nav_host_fragment_content_main)
//        val currentDestination = navController.currentDestination
//
//            if (currentDestination?.id == R.id.nav_home || currentDestination?.id == R.id.calendarYear1Fragment) {
//                when(item.itemId) {R.id.action_settings -> {;return true } }
//            }
//
//        return super.onOptionsItemSelected(item)
//    }
//    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
//        val navController = findNavController(R.id.nav_host_fragment_content_main)
//        val currentDestination = navController.currentDestination
//
//        // Adjust menu visibility dynamically
//        menu?.findItem(R.id.action_settings)?.isVisible = currentDestination?.id == R.id.nav_home || currentDestination?.id == R.id.calendarYear1Fragment
//
//        return super.onPrepareOptionsMenu(menu)
//    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}