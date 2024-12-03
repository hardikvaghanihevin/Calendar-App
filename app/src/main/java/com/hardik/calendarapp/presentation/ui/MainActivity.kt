package com.hardik.calendarapp.presentation.ui

import android.os.Bundle
import android.util.Log
import android.view.View
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
        //Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG).setAction("Action", null).setAnchorView(R.id.fab).show()
        // Check current fragment
        //val currentDestination = navController.currentDestination?.id
        //if (currentDestination != R.id.newEventFragment)

        binding.appBarMain.fab.setOnClickListener { view ->
            navController.navigate(R.id.newEventFragment)
        }

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
        navController.addOnDestinationChangedListener { _, destination, _ ->
            invalidateOptionsMenu()
            val isFabVisible = destination.id != R.id.newEventFragment

            if (isFabVisible) {
                showFabWithAnimation(binding.appBarMain.fab)
            } else {
                hideFabWithAnimation(binding.appBarMain.fab)
            }
        }
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
    // Function to show FAB with animation
    private fun showFabWithAnimation(fab: View) {
        fab.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(200) // animation duration in milliseconds
            .withStartAction { fab.visibility = View.VISIBLE }
            .start()
    }

    // Function to hide FAB with animation
    private fun hideFabWithAnimation(fab: View) {
        fab.animate()
            .alpha(0f)
            .scaleX(0f)
            .scaleY(0f)
            .setDuration(200) // animation duration in milliseconds
            .withEndAction { fab.visibility = View.GONE }
            .start()
    }
}