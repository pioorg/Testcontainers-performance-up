/*
 *  Copyright (C) 2023 Piotr Przybył
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package dev.softwaregarden.tcPerfUp;

import dev.softwaregarden.tcPerfUp.misc.FakeSleeper;
import org.junit.jupiter.api.Test;


class TestF {

    @Test
    void test1() {
        System.out.println("test F1");
        FakeSleeper.sleep();
    }

    @Test
    void test2() {
        System.out.println("test F2");
        FakeSleeper.sleep();
    }
}