/*
 * Copyright 2026 Kazimierz Pogoda / Xemantic
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.xemantic.website

import com.xemantic.markanywhere.SemanticEvent
import com.xemantic.markanywhere.transform.transform
import kotlinx.coroutines.flow.Flow

/**
 * Completes every `img` of the content of the [page]:
 *
 * - an image published by the site gets its `width` and `height`, read from
 *   its header (see [readImageSize]), so that the browser reserves its space
 *   before it arrives, instead of shifting the page content down once it does;
 *   a reference to a missing site file fails the build,
 * - every image but the first one, likely above the fold, is loaded lazily,
 *   only once it is about to be scrolled into view.
 *
 * Attributes given in the source are kept. It has to run before
 * [applyContentWidgets], which copies the attributes of a profile photograph.
 */
fun Flow<SemanticEvent>.completeImages(page: Page): Flow<SemanticEvent> = transform {

    var first = true

    match("img") { mark ->
        val size = mark["src"]?.let { page.siteFile(page.resolveUrl(it)) }?.readImageSize()
        val attributes = buildMap {
            size?.let { (width, height) ->
                put("width", width.toString())
                put("height", height.toString())
            }
            if (!first) {
                put("loading", "lazy")
                put("decoding", "async")
            }
            putAll(mark.attributes)
        }
        first = false
        "img"(attributes) {
            children()
        }
    }

    passthrough()

}
