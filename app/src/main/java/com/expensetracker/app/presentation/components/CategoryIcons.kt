package com.expensetracker.app.presentation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Book
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.Checkroom
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Coffee
import androidx.compose.material.icons.rounded.DirectionsBus
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Kitchen
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.LocalGasStation
import androidx.compose.material.icons.rounded.LocalPizza
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.PhoneIphone
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Train
import androidx.compose.material.icons.rounded.Watch
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Fixed, discoverable icon set for categories. Categories store a [String] key
 * in the database; the UI resolves it here. Unknown keys fall back to a dot.
 */
object CategoryIcons {

    val all: Map<String, ImageVector> = linkedMapOf(
        "restaurant" to Icons.Rounded.Restaurant,
        "local_pizza" to Icons.Rounded.LocalPizza,
        "coffee" to Icons.Rounded.Coffee,
        "cart" to Icons.Rounded.ShoppingCart,
        "shopping_bag" to Icons.Rounded.ShoppingBag,
        "directions_bus" to Icons.Rounded.DirectionsBus,
        "directions_car" to Icons.Rounded.DirectionsCar,
        "train" to Icons.Rounded.Train,
        "local_gas_station" to Icons.Rounded.LocalGasStation,
        "movie" to Icons.Rounded.Movie,
        "sports_esports" to Icons.Rounded.SportsEsports,
        "music_note" to Icons.Rounded.MusicNote,
        "school" to Icons.Rounded.School,
        "book" to Icons.Rounded.Book,
        "favorite" to Icons.Rounded.Favorite,
        "fitness_center" to Icons.Rounded.FitnessCenter,
        "checkroom" to Icons.Rounded.Checkroom,
        "watch" to Icons.Rounded.Watch,
        "phone_iphone" to Icons.Rounded.PhoneIphone,
        "camera_alt" to Icons.Rounded.CameraAlt,
        "headphones" to Icons.Rounded.MusicNote,
        "kitchen" to Icons.Rounded.Kitchen,
        "flight" to Icons.Rounded.Flight,
        "card_giftcard" to Icons.Rounded.CardGiftcard,
        "receipt_long" to Icons.Rounded.ReceiptLong,
        "home" to Icons.Rounded.Home,
        "bolt" to Icons.Rounded.Bolt,
        "wifi" to Icons.Rounded.Wifi,
        "cloud" to Icons.Rounded.Cloud,
        "language" to Icons.Rounded.Language,
        "calendar_month" to Icons.Rounded.CalendarMonth,
        "lock" to Icons.Rounded.Lock,
        "pets" to Icons.Rounded.Pets,
        "build" to Icons.Rounded.Build,
        "star" to Icons.Rounded.Star,
        "auto_awesome" to Icons.Rounded.AutoAwesome,
        "more_horiz" to Icons.Rounded.MoreHoriz,
    )

    fun iconFor(key: String): ImageVector = all[key] ?: Icons.Rounded.MoreHoriz
}
