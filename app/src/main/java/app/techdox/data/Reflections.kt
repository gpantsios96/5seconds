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

    private var last = -1

    /** A random index into [all], never the same one twice in a row. */
    fun pickIndex(random: Random = Random.Default): Int {
        var i = random.nextInt(all.size)
        if (i == last) i = (i + 1) % all.size
        last = i
        return i
    }

    fun text(index: Int, appLabel: String): String =
        all[index.mod(all.size)].replace("%s", appLabel)
}
