package app.techdox.data

import kotlin.random.Random

/**
 * Short reflection lines shown on the pause card. All of them are original to this project.
 * `%s` is replaced with the name of the app being opened.
 */
object Reflections {
    val all: List<String> = listOf(
        "What were you hoping to find in %s?",
        "Is this a choice, or just a habit?",
        "The feed will still be there later. Will this moment?",
        "What were you doing ten seconds ago?",
        "If you open %s now, what are you putting off?",
        "Boredom is not an emergency.",
        "You picked up the phone for a reason. Was this it?",
        "Nothing new has happened in %s that needs you right now.",
        "How do you usually feel after twenty minutes in %s?",
        "Your attention is the most valuable thing you'll spend today.",
        "Reaching for the phone is a feeling. Name it first.",
        "One small 'not now' is how a different day starts.",
        "What would you do with the next fifteen minutes if %s didn't exist?",
        "Tired, bored, anxious or lonely? %s fixes none of them.",
        "Scrolling is easy to start and hard to remember.",
        "Is there someone you could message instead of everyone you could watch?",
        "You don't have to check. Nothing is waiting on you.",
        "The thing you're avoiding will still be there after %s.",
        "A minute here is rarely a minute.",
        "What would the person you want to be do right now?",
        "Rest is not the same thing as scrolling.",
        "You've been here before today. Did it help?",
        "Look up. What is actually around you?",
        "If this is a break, decide how long it is before you start.",
        "Curiosity or reflex?",
        "The best part of %s is usually the first two minutes.",
        "What's one thing you'd be glad you did instead?",
        "You are allowed to be a little bored.",
        "Every open is a vote for how you spend your days.",
        "Open %s on purpose, or not at all.",
        "Is this what you want, or just what's closest?",
        "Tonight, will you remember what you saw here?",
        "A deep breath costs less than a deep scroll.",
        "Finish the thing first. Then come back if you still want to.",
        "Your thumb got here before you did.",
        "Small pauses add up to a different week.",
    )

    /** What the pause card shows: one of the user's own reasons, or a built-in line. */
    data class Card(val text: String, val own: Boolean)

    const val MAX_REASONS = 5
    const val MAX_REASON_LENGTH = 120

    private var lastText: String? = null

    /**
     * Picks the card for one pause.
     * - No reasons written: a built-in line.
     * - Reasons written: one of them; with [mixBuiltIn], a built-in line about one time in three.
     * The same text is never shown twice in a row when there is another one to show.
     */
    fun choose(reasons: List<String>, mixBuiltIn: Boolean, random: Random = Random.Default): Card {
        val useOwn = reasons.isNotEmpty() && (!mixBuiltIn || random.nextInt(3) != 0)
        val pool = if (useOwn) reasons else all
        val candidates = pool.filter { it != lastText }.ifEmpty { pool }
        val text = candidates[random.nextInt(candidates.size)]
        lastText = text
        return Card(text, own = useOwn)
    }

    /** Built-in lines may contain `%s` for the app's name; the user's own words are shown as written. */
    fun display(card: Card, appLabel: String): String =
        if (card.own) card.text else card.text.replace("%s", appLabel)

    /** Cleans up what the user typed: one line each, trimmed, no blanks or duplicates, capped. */
    fun sanitize(reasons: List<String>): List<String> =
        reasons.map { it.replace('\n', ' ').trim().take(MAX_REASON_LENGTH) }
            .filter { it.isNotEmpty() }
            .distinct()
            .take(MAX_REASONS)
}
