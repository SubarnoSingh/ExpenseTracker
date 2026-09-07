package com.expensetracker.app.data.sms

/**
 * Guesses which category an SMS debit belongs to from the merchant name.
 *
 * Returns a name from [com.expensetracker.app.data.local.DefaultCategories] - the
 * caller matches it against the user's real categories and falls back when there
 * is no match, so renaming or deleting a default category degrades to "unsorted"
 * rather than breaking.
 *
 * Subscription rules are checked first and only fire when the message itself
 * looks recurring, so a one-off Google Play purchase doesn't become a standing
 * subscription.
 */
object SmsCategoryGuess {

    /** Words that mark a debit as recurring rather than one-off. */
    private val RECURRING = Regex(
        """\b(subscription|subscribed|auto[- ]?pay|autopay|auto[- ]?deb(it|ited)|recurring|renew(ed|al)?|mandate|standing instruction|si debit|monthly plan|annual plan)\b""",
        RegexOption.IGNORE_CASE,
    )

    private val SUBSCRIPTION_RULES = listOf(
        rule("Music & Video", "netflix", "spotify", "hotstar", "prime video", "youtube", "jiocinema", "sonyliv", "zee5", "gaana", "wynk", "apple music", "audible"),
        rule("Software & Cloud", "google one", "google cloud", "google storage", "icloud", "adobe", "microsoft", "office 365", "dropbox", "github", "openai", "chatgpt", "anthropic", "claude", "notion", "figma", "canva", "jetbrains", "aws", "amazon web"),
        rule("Domains & Hosting", "godaddy", "namecheap", "hostinger", "bigrock", "digitalocean", "vercel", "netlify", "cloudflare", "domain", "hosting"),
        rule("Memberships", "gym", "cult", "fitness", "membership", "swiggy one", "zomato gold", "amazon prime", "flipkart plus"),
    )

    private val EXPENSE_RULES = listOf(
        // ---- Regular ----
        rule("Food & Drinks", "swiggy", "zomato", "domino", "pizza", "mcdonald", "kfc", "burger", "cafe", "coffee", "starbucks", "restaurant", "barbeque", "bbq", "biryani", "dhaba", "bakery", "eatery", "food"),
        rule("Groceries", "bigbasket", "blinkit", "zepto", "dmart", "d-mart", "grofers", "instamart", "reliance fresh", "supermarket", "grocery", "kirana", "provision"),
        rule("Transport", "uber", "ola ", "olacabs", "rapido", "namma yatri", "irctc", "redbus", "metro", "bmtc", "cab", "rickshaw", "toll", "fastag"),
        rule("Fuel", "petrol", "fuel", "hpcl", "iocl", "bpcl", "indian oil", "bharat petroleum", "shell", "nayara"),
        rule("Entertainment", "bookmyshow", "pvr", "inox", "cinepolis", "steam", "playstation", "xbox", "epic games", "nintendo", "cinema"),
        rule("Education", "udemy", "coursera", "byju", "unacademy", "vedantu", "physics wallah", "school", "college", "tuition", "course"),
        rule("Health", "pharmacy", "apollo", "medplus", "1mg", "pharmeasy", "netmeds", "hospital", "clinic", "diagnostic", "pathology", "medical", "dental"),
        rule("Shopping", "amazon", "flipkart", "myntra", "ajio", "meesho", "nykaa", "snapdeal", "tata cliq"),
        rule("Bills", "electricity", "bescom", "tata power", "adani", "broadband", "airtel", "jio", "vodafone", "bsnl", "recharge", "dth", "water bill", "gas bill", "billdesk"),
        // ---- Occasional ----
        rule("Clothes", "zara", "h&m", "uniqlo", "levis", "max fashion", "westside", "pantaloons", "shoppers stop", "decathlon", "apparel", "fashion", "clothing"),
        rule("Electronics", "croma", "reliance digital", "vijay sales", "boat", "noise", "oneplus", "samsung", "headphone", "laptop"),
        rule("Travel", "makemytrip", "goibibo", "yatra", "cleartrip", "ixigo", "airbnb", "oyo", "booking.com", "indigo", "vistara", "air india", "spicejet", "akasa", "hotel", "flight", "travel", "tourism"),
        rule("Gifts", "ferns", "archies", "gift"),
    )

    /**
     * @param merchant the merchant [SmsParser] pulled out
     * @param body the whole message - recurring wording usually lives outside the merchant name
     * @return a default category name, or null when nothing matches
     */
    fun guess(merchant: String, body: String): String? {
        val haystack = merchant.lowercase()

        // A known subscription brand is a subscription with or without recurring wording.
        SUBSCRIPTION_RULES.firstOrNull { (_, keywords) -> keywords.any { it in haystack } }
            ?.let { return it.first }

        // Recurring wording but an unknown brand - still a subscription, just unsorted.
        if (RECURRING.containsMatchIn(body)) return "Other Services"

        return EXPENSE_RULES.firstOrNull { (_, keywords) -> keywords.any { it in haystack } }?.first
    }

    private fun rule(category: String, vararg keywords: String): Pair<String, List<String>> =
        category to keywords.toList()
}
