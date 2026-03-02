package com.micik.kittygram.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.times

@Composable
fun ProfileScreen(
    modifier: Modifier = Modifier,
    likedImages: List<String>,
    savedImages: List<String>,
    apiPosts: List<String>
) {
    var username by remember { mutableStateOf("You") }
    var editing by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var imagesReceivedCount by remember { mutableStateOf(0) }

    // Load persisted count on first composition
    LaunchedEffect(Unit) {
        val prefs = context.getSharedPreferences("kittygram_prefs", android.content.Context.MODE_PRIVATE)
        imagesReceivedCount = prefs.getInt("api_posts_count", 0)
    }

    // Increment and persist count if new images are received
    LaunchedEffect(apiPosts.size) {
        val prefs = context.getSharedPreferences("kittygram_prefs", android.content.Context.MODE_PRIVATE)
        if (apiPosts.size > imagesReceivedCount) {
            imagesReceivedCount = apiPosts.size
            prefs.edit().putInt("api_posts_count", imagesReceivedCount).apply()
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(horizontal = 24.dp)
    ) {

        // Profile Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp, bottom = 16.dp),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = painterResource(id = com.micik.kittygram.R.drawable.ic_launcher_foreground),
                        contentDescription = "Profile picture",
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(Color.LightGray),
                        contentScale = ContentScale.Crop
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (editing) {
                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it },
                            singleLine = true,
                            label = { Text("Username") },
                            modifier = Modifier.width(220.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { editing = false }) {
                            Text("Save")
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = username,
                                fontWeight = FontWeight.Bold,
                                fontSize = 24.sp
                            )
//                            Spacer(modifier = Modifier.width(8.dp))
//                            IconButton(onClick = { editing = true }) {
//                                Icon(
//                                    painter = painterResource(id = com.micik.kittygram.R.drawable.ic_launcher_foreground),
//                                    contentDescription = "Edit username",
//                                    modifier = Modifier.size(20.dp)
//                                )
//                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    Divider(thickness = 1.dp, color = Color.LightGray.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        ProfileStat("Liked", likedImages.size)
                        ProfileStat("Bookmarked", savedImages.size)
                        ProfileStat("Posts", imagesReceivedCount)
                    }
                }
            }
        }

        // Liked Section
        if (likedImages.isNotEmpty()) {
            item {
                SectionHeader("Liked Posts")
            }

            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2 * 120.dp)
                ) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(likedImages) { url ->
                            AsyncImage(
                                model = url,
                                contentDescription = null,
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // Saved Section
        if (savedImages.isNotEmpty()) {
            item {
                SectionHeader("Bookmarked Posts")
            }

            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2 * 120.dp)
                ) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(savedImages) { url ->
                            AsyncImage(
                                model = url,
                                contentDescription = null,
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun ProfileStat(label: String, count: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = "$count", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text(text = label, fontSize = 14.sp, color = Color.Gray)
    }
}

@Composable
fun SectionHeader(title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.primary
        )
    }
}
