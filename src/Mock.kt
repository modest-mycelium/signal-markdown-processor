// Copyright (C) 2025 Modest Mycelium
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU Affero General Public License as
// published by the Free Software Foundation, either version 3 of the
// License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU Affero General Public License for more details.
//
// You should have received a copy of the GNU Affero General Public License
// along with this program.  If not, see <https://www.gnu.org/licenses/>.

// The following classes in this file are defined loosely based on their definitions in the Signal-Android code repository.
//
// BodyRange, Copyright 2013-2025 Signal Messenger, LLC, available under the AGPL-3.0-only license, found at
// https://github.com/signalapp/Signal-Android/blob/80598d42cc169379829fb628a6ddd8ee57731b3a/app/src/main/protowire/Backup.proto#L836
//
// BodyRangeList, Copyright (C) 2014-2016 Open Whisper Systems, available under the AGPL-3.0-only license, found at
// https://github.com/signalapp/Signal-Android/blob/80598d42cc169379829fb628a6ddd8ee57731b3a/app/src/main/protowire/Database.proto#L89

class ByteString {
    companion object {
        val EMPTY = ByteString()
    }
}

enum class Style {
    NONE, BOLD, ITALIC, SPOILER, STRIKETHROUGH, MONOSPACE;
}

data class BodyRange(
    val start: Int = 0,
    val length: Int = 0,
    val mentionAci: ByteString? = null,
    val style: Style? = null,
    val unknownFields: ByteString = ByteString.EMPTY,
) {
    override fun toString(): String {
        return "BodyRange(start=$start, length=$length, style=$style)"
    }
}

data class BodyRangeList(
    val ranges: List<BodyRange> = emptyList(),
    val unknownFields: ByteString = ByteString.EMPTY,
) {
    override fun toString(): String {
        return "BodyRangeList(ranges=$ranges)"
    }

    override fun equals(other: Any?): Boolean {
        if (other !is BodyRangeList) return false
        return ranges.toSet() == other.ranges.toSet()
    }

    override fun hashCode(): Int {
        return ranges.toSet().hashCode()
    }
}
