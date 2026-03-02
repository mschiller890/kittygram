package com.micik.kittygram.ui

import android.os.Environment
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ForYouScreen(
    modifier: Modifier = Modifier,
    likedImages: MutableList<String>,
    savedImages: MutableList<String>,
    apiPosts: MutableList<String>,
    onImageClick: (String) -> Unit = {},
    catImages: androidx.compose.runtime.snapshots.SnapshotStateList<CatImage>,
    isLoading: MutableState<Boolean>,
    listState: LazyListState
) {
    val scope = rememberCoroutineScope()

    // State for modal
    var selectedPost by remember { mutableStateOf<CatImage?>(null) }

    fun loadMoreCats() {
        if (isLoading.value) return
        isLoading.value = true
        scope.launch {
            try {
                val images = CatApi.service.getCatImages(limit = 10, apiKey = null, breedIds = null)
                catImages.addAll(images)
                images.forEach { if (!apiPosts.contains(it.url)) apiPosts.add(it.url) }
            } catch (_: Exception) { }
            isLoading.value = false
        }
    }

    LaunchedEffect(Unit) { loadMoreCats() }

    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .map { it == catImages.lastIndex }
            .distinctUntilChanged()
            .filter { it }
            .collectLatest { loadMoreCats() }
    }

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp)
    ) {
        items(catImages) { cat ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .padding(vertical = 6.dp),
                shape = MaterialTheme.shapes.medium,
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    // Heart animation (multiple hearts)
                    data class HeartState(
                        val id: Long,
                        val offset: Offset,
                        val scale: Animatable<Float, AnimationVector1D>,
                        val alpha: Animatable<Float, AnimationVector1D>,
                        val rotation: Animatable<Float, AnimationVector1D>
                    )

                    var hearts by remember { mutableStateOf(listOf<HeartState>()) }

                    // Download message
                    var downloadMessage by remember { mutableStateOf("") }
                    var showDownloadMessage by remember { mutableStateOf(false) }

                    val context = LocalContext.current

                    // Image gestures
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(cat.url) {
                                detectTapGestures(
                                    onTap = {
                                        selectedPost = cat
                                    },
                                    onDoubleTap = { offset ->
                                        if (!likedImages.contains(cat.url)) likedImages.add(cat.url)

                                        val id = System.currentTimeMillis() + (0..1000).random()
                                        val scale = Animatable(0f)
                                        val alpha = Animatable(0f)
                                        val rotation = Animatable(0f)
                                        val heart = HeartState(id, offset, scale, alpha, rotation)
                                        hearts = hearts + heart

                                        scope.launch {
                                            scale.snapTo(0f)
                                            alpha.snapTo(0f)
                                            rotation.snapTo(0f)
                                            alpha.animateTo(1f, tween(100, easing = LinearEasing))
                                            launch {
                                                scale.animateTo(
                                                    2.5f,
                                                    spring(stiffness = 300f, dampingRatio = 0.4f)
                                                )
                                            }
                                            launch {
                                                rotation.animateTo(-15f, tween(100))
                                                rotation.animateTo(15f, tween(100))
                                                rotation.animateTo(0f, tween(100))
                                            }
                                            kotlinx.coroutines.delay(700)
                                            alpha.animateTo(0f, tween(300))
                                            hearts = hearts.filter { it.id != id }
                                        }
                                    }
                                )
                            }
                    ) {
                        // Blurred background
                        AsyncImage(
                            model = cat.url,
                            contentDescription = null,
                            modifier = Modifier
                                .matchParentSize()
                                .graphicsLayer { scaleX = 1.1f; scaleY = 1.1f }
                                .blur(30.dp),
                            contentScale = ContentScale.Crop,
                            alpha = 0.6f
                        )

                        // Foreground image
                        AsyncImage(
                            model = cat.url,
                            contentDescription = "Cat Image",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )

                        // Heart animation (multiple hearts)
                        hearts.forEach { heart ->
                            Icon(
                                imageVector = Icons.Filled.Favorite,
                                contentDescription = null,
                                tint = Color.Red,
                                modifier = Modifier
                                    .offset {
                                        IntOffset(
                                            x = (heart.offset.x - 24.dp.toPx() / 2).toInt(),
                                            y = (heart.offset.y - 24.dp.toPx() / 2).toInt()
                                        )
                                    }
                                    .size(48.dp)
                                    .graphicsLayer(
                                        scaleX = heart.scale.value,
                                        scaleY = heart.scale.value,
                                        alpha = heart.alpha.value,
                                        rotationZ = heart.rotation.value
                                    )
                            )
                        }

                        // Top-start: Download button
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(8.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.Black.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.padding(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = {
                                    scope.launch {
                                        val activity = context as? android.app.Activity
                                        val fileName = "kittygram_${System.currentTimeMillis()}.jpg"
                                        val result = withContext(Dispatchers.IO) {
                                            try {
                                                val url = URL(cat.url)
                                                val connection = url.openConnection()
                                                connection.connect()
                                                val input = connection.getInputStream()
                                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                                                    val resolver = context.contentResolver
                                                    val contentValues =
                                                        android.content.ContentValues().apply {
                                                            put(
                                                                android.provider.MediaStore.Images.Media.DISPLAY_NAME,
                                                                fileName
                                                            )
                                                            put(
                                                                android.provider.MediaStore.Images.Media.MIME_TYPE,
                                                                "image/jpeg"
                                                            )
                                                            put(
                                                                android.provider.MediaStore.Images.Media.RELATIVE_PATH,
                                                                "Pictures/Kittygram"
                                                            )
                                                        }
                                                    val uri = resolver.insert(
                                                        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                                        contentValues
                                                    )
                                                    if (uri != null) {
                                                        resolver.openOutputStream(uri)
                                                            ?.use { output -> input.copyTo(output) }
                                                        "Photo downloaded"
                                                    } else "Download failed"
                                                } else {
                                                    val permission =
                                                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                                                    val granted =
                                                        androidx.core.content.ContextCompat.checkSelfPermission(
                                                            context,
                                                            permission
                                                        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                                                    if (activity != null && !granted) {
                                                        withContext(Dispatchers.Main) {
                                                            androidx.core.app.ActivityCompat.requestPermissions(
                                                                activity,
                                                                arrayOf(permission),
                                                                1001
                                                            )
                                                        }
                                                        return@withContext "Please grant storage permission and try again"
                                                    }
                                                    val picturesDir =
                                                        Environment.getExternalStoragePublicDirectory(
                                                            Environment.DIRECTORY_PICTURES
                                                        )
                                                    val file = File(picturesDir, fileName)
                                                    FileOutputStream(file).use { output ->
                                                        input.copyTo(
                                                            output
                                                        )
                                                    }
                                                    "Photo downloaded"
                                                }
                                            } catch (e: Exception) {
                                                "Download failed: ${e.message}"
                                            }
                                        }

                                        downloadMessage = result
                                        showDownloadMessage = true
                                        kotlinx.coroutines.delay(2000)
                                        showDownloadMessage = false
                                    }
                                }) {
                                    Icon(
                                        imageVector = Icons.Outlined.Download,
                                        contentDescription = "Download",
                                        tint = Color.White
                                    )
                                }
                            }
                        }

                        // Top-end: Like & Bookmark buttons
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.Black.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.padding(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = {
                                    if (likedImages.contains(cat.url)) likedImages.remove(cat.url)
                                    else likedImages.add(cat.url)
                                }) {
                                    Icon(
                                        imageVector = if (likedImages.contains(cat.url)) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                        contentDescription = "Like",
                                        tint = if (likedImages.contains(cat.url)) Color.Red else Color.White
                                    )
                                }
                                IconButton(onClick = {
                                    if (savedImages.contains(cat.url)) savedImages.remove(cat.url)
                                    else savedImages.add(cat.url)
                                }) {
                                    Icon(
                                        imageVector = if (savedImages.contains(cat.url)) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                                        contentDescription = "Bookmark",
                                        tint = Color.White
                                    )
                                }
                            }
                        }

                        // Bottom download message sliding from bottom of post
                        Column(modifier = Modifier.fillMaxSize()) {
                            Spacer(modifier = Modifier.weight(1f))
                            AnimatedVisibility(
                                visible = showDownloadMessage,
                                enter = slideInVertically(
                                    initialOffsetY = { it },
                                    animationSpec = tween(300)
                                ) + fadeIn(animationSpec = tween(300)),
                                exit = slideOutVertically(
                                    targetOffsetY = { it },
                                    animationSpec = tween(300)
                                ) + fadeOut(animationSpec = tween(300))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.Black.copy(alpha = 0.6f))
                                        .padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = downloadMessage,
                                        color = Color.White,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (isLoading.value) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}