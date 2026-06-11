package com.example.tarifdefterim

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONObject
import java.net.URL

class MainActivity : ComponentActivity() {
    private lateinit var dbHelper: FavoriteDbHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dbHelper = FavoriteDbHelper(this)

        setContent {
            TarifDefterimApp(dbHelper)
        }
    }
}

data class Meal(val name: String, val instructions: String)

class FavoriteDbHelper(context: Context) :
    SQLiteOpenHelper(context, "favorites.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE favorites (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, instructions TEXT)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS favorites")
        onCreate(db)
    }

    // Favori tarif ekler
    fun addFavorite(meal: Meal) {
        val values = ContentValues()
        values.put("name", meal.name)
        values.put("instructions", meal.instructions)
        writableDatabase.insert("favorites", null, values)
    }

    // Favori tarifleri listeler
    fun getFavorites(): List<Meal> {
        val list = mutableListOf<Meal>()
        val cursor = readableDatabase.rawQuery("SELECT name, instructions FROM favorites", null)

        while (cursor.moveToNext()) {
            list.add(Meal(cursor.getString(0), cursor.getString(1)))
        }

        cursor.close()
        return list
    }
}

val Cream = Color(0xFFFFF3E0)
val Orange = Color(0xFFFF8A00)
val Brown = Color(0xFF5D4037)
val CardColor = Color(0xFFFFE0B2)

@Composable
fun TarifDefterimApp(dbHelper: FavoriteDbHelper) {
    var screen by remember { mutableStateOf("home") }
    var selectedMeal by remember { mutableStateOf<Meal?>(null) }

    when (screen) {
        "home" -> HomeScreen(
            onMealClick = {
                selectedMeal = it
                screen = "detail"
            },
            onFavoritesClick = { screen = "favorites" }
        )

        "detail" -> DetailScreen(
            meal = selectedMeal,
            dbHelper = dbHelper,
            onBackClick = { screen = "home" }
        )

        "favorites" -> FavoritesScreen(
            dbHelper = dbHelper,
            onBackClick = { screen = "home" }
        )
    }
}

@Composable
fun HomeScreen(onMealClick: (Meal) -> Unit, onFavoritesClick: () -> Unit) {
    var meals by remember { mutableStateOf(listOf<Meal>()) }
    var loading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Cream)
            .padding(20.dp)
    ) {
        Text("🍽️ Tarif Defterim", fontSize = 32.sp, color = Brown)
        Text("Lezzetli tarifleri keşfet ve favorilerine ekle.", color = Brown)

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Orange),
            onClick = {
                loading = true

                Thread {
                    try {
                        val result = URL("https://www.themealdb.com/api/json/v1/1/search.php?s=chicken").readText()
                        val json = JSONObject(result)
                        val mealsArray = json.getJSONArray("meals")
                        val tempList = mutableListOf<Meal>()

                        for (i in 0 until mealsArray.length()) {
                            val item = mealsArray.getJSONObject(i)
                            tempList.add(
                                Meal(
                                    item.getString("strMeal"),
                                    item.getString("strInstructions")
                                )
                            )
                        }

                        meals = tempList
                        loading = false
                    } catch (e: Exception) {
                        loading = false
                    }
                }.start()
            }
        ) {
            Text("API'den Tarifleri Getir")
        }

        Spacer(modifier = Modifier.height(10.dp))

        Button(
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Brown),
            onClick = onFavoritesClick
        ) {
            Text("Favorilerim")
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (loading) {
            Text("Tarifler yükleniyor...", color = Brown)
        }

        LazyColumn {
            items(meals) { meal ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    colors = CardDefaults.cardColors(containerColor = CardColor),
                    onClick = { onMealClick(meal) }
                ) {
                    Text(
                        text = "🍴 ${meal.name}",
                        modifier = Modifier.padding(16.dp),
                        fontSize = 18.sp,
                        color = Brown
                    )
                }
            }
        }
    }
}

@Composable
fun DetailScreen(meal: Meal?, dbHelper: FavoriteDbHelper, onBackClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Cream)
            .padding(20.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("📖 Tarif Detayı", fontSize = 30.sp, color = Brown)

        Spacer(modifier = Modifier.height(16.dp))

        Card(colors = CardDefaults.cardColors(containerColor = CardColor)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(meal?.name ?: "Tarif seçilmedi", fontSize = 24.sp, color = Brown)

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = meal?.instructions ?: "",
                    color = Brown
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Orange),
            onClick = {
                if (meal != null) {
                    dbHelper.addFavorite(meal)
                }
            }
        ) {
            Text("Favorilere Ekle")
        }

        Spacer(modifier = Modifier.height(10.dp))

        Button(
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Brown),
            onClick = onBackClick
        ) {
            Text("Geri Dön")
        }
    }
}

@Composable
fun FavoritesScreen(dbHelper: FavoriteDbHelper, onBackClick: () -> Unit) {
    var favorites by remember { mutableStateOf(dbHelper.getFavorites()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Cream)
            .padding(20.dp)
    ) {
        Text("⭐ Favori Tarifler", fontSize = 30.sp, color = Brown)

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Orange),
            onClick = { favorites = dbHelper.getFavorites() }
        ) {
            Text("Favorileri Yenile")
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn {
            items(favorites) { meal ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    colors = CardDefaults.cardColors(containerColor = CardColor)
                ) {
                    Text(
                        text = "❤️ ${meal.name}",
                        modifier = Modifier.padding(16.dp),
                        fontSize = 18.sp,
                        color = Brown
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Button(
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Brown),
            onClick = onBackClick
        ) {
            Text("Geri Dön")
        }
    }
}



