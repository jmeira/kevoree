/**
 * Licensed under the GNU LESSER GENERAL PUBLIC LICENSE, Version 3, 29 June 2007;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	http://www.gnu.org/licenses/lgpl-3.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.kevoree.tools.marShell.interpreter.sub

import org.kevoree.tools.marShell.interpreter.KevsAbstractInterpreter
import org.kevoree.tools.marShell.interpreter.KevsInterpreterContext
import org.kevoree.tools.marShell.ast.{RemoveLibraryStatment}
import scala.collection.JavaConversions._

case class KevsRemoveLibraryInterpreter(statment : RemoveLibraryStatment) extends KevsAbstractInterpreter {

  def interpret(context : KevsInterpreterContext):Boolean={
    context.model.getLibraries.find(lib=> lib.getName == statment.libraryName) match {
      case Some(library) =>  context.model.getLibraries.remove(library);true
      case None => {
        println("Error : Library not found with name "+statment.libraryName)
        false
      }
    }
  }

}
