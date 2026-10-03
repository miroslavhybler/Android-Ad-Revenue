package mir.oslav.adrevenue.utils

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update


/**
 * @author Miroslav Hýbler <br>
 * created on 01.10.2026
 */
/** Atomically updates this state by creating a copy of its current value. */
inline fun <T> MutableStateFlow<T>.updateCopy(crossinline block: T.() -> T) {
    update { current -> current.block() }
}
