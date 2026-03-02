package com.micik.kittygram

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.micik.kittygram.ui.theme.KittygramTheme
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Home
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateListOf
import android.content.Context
import android.content.SharedPreferences
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.composable
import org.json.JSONArray
import com.micik.kittygram.ui.ForYouScreen
import com.micik.kittygram.ui.ProfileScreen
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.micik.kittygram.ui.CatImage
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KittygramTheme {
                MainScreen()
            }
        }
    }
}


@Composable
fun MainScreen() {
    // ForYouScreen persistent state
    val forYouCatImages = remember { mutableStateListOf<CatImage>() }
    val forYouIsLoading = remember { mutableStateOf(false) }
    val forYouListState = androidx.compose.foundation.lazy.rememberLazyListState()
    val navController = rememberNavController()
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences("profile_counts", Context.MODE_PRIVATE) }
    var selectedIndex by remember { mutableStateOf(0) }
    val likedImages = remember { mutableStateListOf<String>() }
    val savedImages = remember { mutableStateListOf<String>() }
    val apiPosts = remember { mutableStateListOf<String>() }

    // Restore liked and bookmarked image URLs on launch
    LaunchedEffect(Unit) {
        fun restoreList(key: String, list: MutableList<String>) {
            val json = prefs.getString(key, null)
            if (json != null) {
                val arr = JSONArray(json)
                for (i in 0 until arr.length()) {
                    list.add(arr.getString(i))
                }
            }
        }
        restoreList("liked_images", likedImages)
        restoreList("bookmarked_images", savedImages)
    }

    // Save liked and bookmarked image URLs when they change
    fun saveLists() {
        fun saveList(key: String, list: List<String>) {
            val arr = JSONArray()
            list.forEach { arr.put(it) }
            prefs.edit().putString(key, arr.toString()).apply()
        }
        saveList("liked_images", likedImages)
        saveList("bookmarked_images", savedImages)
    }

    LaunchedEffect(likedImages.size, savedImages.size) {
        saveLists()
    }
    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Filled.Home, contentDescription = "For You") },
                    label = { Text("For you") },
                    selected = selectedIndex == 0,
                    onClick = { selectedIndex = 0 }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Filled.Person, contentDescription = "Profile") },
                    label = { Text("Profile") },
                    selected = selectedIndex == 2,
                    onClick = { selectedIndex = 2 }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = if (selectedIndex == 0) "forYou" else "profile",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("forYou") {
                ForYouScreen(
                    likedImages = likedImages,
                    savedImages = savedImages,
                    apiPosts = apiPosts,
                    modifier = Modifier.fillMaxSize(),
                    onImageClick = { url ->
                        navController.navigate("imageDetail?imageUrl=$url&details=Details%20about%20this%20image")
                    },
                    catImages = forYouCatImages,
                    isLoading = forYouIsLoading,
                    listState = forYouListState
                )
            }
            composable("profile") {
                ProfileScreen(
                    likedImages = likedImages,
                    savedImages = savedImages,
                    apiPosts = apiPosts,
                    modifier = Modifier.fillMaxSize(),

                )
            }
            composable("imageDetail?imageUrl={imageUrl}&details={details}") { backStackEntry ->
                val imageUrl = backStackEntry.arguments?.getString("imageUrl") ?: ""
                val details = backStackEntry.arguments?.getString("details") ?: ""
                ExplodeTransitionBox(visible = true) {
                    AnimatedImageDetailScreen(
                        imageUrl = imageUrl,
                        details = details,
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    KittygramTheme {
        MainScreen()
    }
}

@Composable
fun ExplodeTransitionBox(
    visible: Boolean,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = scaleIn(
            initialScale = 0.1f,
            animationSpec = tween(600)
        ) + fadeIn(animationSpec = tween(600)),
        exit = scaleOut(
            targetScale = 0.1f,
            animationSpec = tween(600)
        ) + fadeOut(animationSpec = tween(600))
    ) {
        content()
    }
}

@Composable
fun AnimatedImageDetailScreen(
    imageUrl: String,
    details: String,
    onBack: () -> Unit
) {
    var startAnimation by remember { mutableStateOf(false) }

    val offsetY by animateDpAsState(
        targetValue = if (startAnimation) 0.dp else 400.dp,
        animationSpec = tween(durationMillis = 600),
        label = ""
    )

    LaunchedEffect(Unit) { startAnimation = true }

    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(top = 48.dp, start = 24.dp, end = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .offset(y = offsetY),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = details,
                color = Color.White,
                fontSize = 18.sp,
                modifier = Modifier.padding(8.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = onBack) {
                Text("Back")
            }
        }
    }
}