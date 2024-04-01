/*
 Copyright 2024 Yehuda Broderick
 */
/*
 This file is part of esson.

 esson is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License
  as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.

 esson is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.

 You should have received a copy of the GNU Lesser General Public License along with esson. If not, see
  <https://www.gnu.org/licenses/>.
 */
package com.LeggoMahEggo.esson.exceptions;

public class JsonParserException extends RuntimeException {
    public JsonParserException(String message) {
        super(message);
    }

    public JsonParserException(String message, Throwable cause) {
        super(message, cause);
    }
}
