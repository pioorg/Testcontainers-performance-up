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

import org.junit.jupiter.api.Test;

class TestIntB extends BaseIntegrationTest{


    @Test
    void test1() {
        checkJohnnyIsHere(mySQL);
        checkJohnnyIsHere(elasticsearch);
        System.out.println("integration test B1");
    }
    @Test
    void test2() {
        checkJohnnyIsHere(mySQL);
        checkJohnnyIsHere(elasticsearch);
        System.out.println("integration test B2");
    }
    @Test
    void test3() {
        checkJohnnyIsHere(mySQL);
        checkJohnnyIsHere(elasticsearch);
        System.out.println("integration test B3");
    }
}