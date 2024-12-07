package com.hypergonial.chat.view.composables

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * A text that can contain hyperlinks. Shamelessly stolen from https://gist.github.com/stevdza-san/ff9dbec0e072d8090e1e6d16e6b73c91
 *
 * @param modifier The modifier to be applied to the text.
 * @param fullText The full text to be displayed.
 * @param hyperLinks A map of the hyperlinks to be displayed.
 * The key is the text to be hyperlinked, and the value is the URL to be opened.
 * @param textStyle The style to be applied to the text.
 * @param linkTextColor The color to be applied to the hyperlinked text.
 * @param linkTextFontWeight The font weight to be applied to the hyperlinked text.
 * @param linkTextDecoration The text decoration to be applied to the hyperlinked text.
 * @param fontSize The font size to be applied to the text.
 * */
@Composable
fun HyperText(
    fullText: String,
    hyperLinks: Map<String, String>,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface),
    linkTextColor: Color = MaterialTheme.colorScheme.primary,
    linkTextFontWeight: FontWeight = FontWeight.Normal,
    linkTextDecoration: TextDecoration = TextDecoration.None,
    fontSize: TextUnit = TextUnit.Unspecified
) {
    val annotatedString = buildAnnotatedString {
        append(fullText)

        for ((text, url) in hyperLinks) {

            val startIndex = fullText.indexOf(text)
            val endIndex = startIndex + text.length
            addStyle(
                style = SpanStyle(
                    color = linkTextColor,
                    fontSize = fontSize,
                    fontWeight = linkTextFontWeight,
                    textDecoration = linkTextDecoration
                ), start = startIndex, end = endIndex
            )
            addLink(LinkAnnotation.Url(url = url), start = startIndex, end = endIndex)
        }
        addStyle(
            style = SpanStyle(
                fontSize = fontSize
            ), start = 0, end = fullText.length
        )
    }

    Text(modifier = modifier, text = annotatedString, style = textStyle)
}
