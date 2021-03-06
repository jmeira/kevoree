/**
 * Licensed under the GNU LESSER GENERAL PUBLIC LICENSE, Version 3, 29 June 2007;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.gnu.org/licenses/lgpl-3.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.kevoree.tools.ui.editor.command

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

import org.kevoree.tools.ui.editor.{UIHelper, KevoreeUIKernel}
import com.explodingpixels.macwidgets.HudWindow
import java.awt.BorderLayout
import java.awt.event.{ActionEvent, ActionListener}
import com.explodingpixels.macwidgets.plaf.{HudLabelUI, HudComboBoxUI}
import javax.swing._
import org.kevoree.tools.ui.editor.form._
import org.kevoree.ComponentType

/**
 * User: ffouquet
 * Date: 13/09/11
 * Time: 13:12
 */

class AddComponentTypeElementUICommand extends Command with PortForm with DictionaryForm {

  def portLabel = "Port"

  def dictionaryLabel = "Dictionnary"

  var kernel: KevoreeUIKernel = null
  var componentType: ComponentType = null

  def setKernel (k: KevoreeUIKernel) {
    kernel = k
  }

  def setComponentType (ct: ComponentType) {
    //logger.debug("ComponentType " + ct)
    this.componentType = ct
  }

  def execute (p: AnyRef) {
    val newPopup = new HudWindow("Add new element to ComponenType")
    newPopup.getJDialog.setSize(400, 200)
    newPopup.getJDialog.setLocationRelativeTo(null)
    val layoutPopup = new JPanel()
    layoutPopup.setOpaque(false)
    layoutPopup.setLayout(new BorderLayout())

    val newElementsModel = new DefaultComboBoxModel
    UIHelper.addItem(newElementsModel,portLabel)
    UIHelper.addItem(newElementsModel,dictionaryLabel)


    if (p != null) {
      newElementsModel.setSelectedItem(p)
    }
    val newElements = UIHelper.createJComboBox(newElementsModel,new HudComboBoxUI())
    val newElementsLabel = new JLabel("Add new : ", SwingConstants.TRAILING)
    newElementsLabel.setUI(new HudLabelUI)
    newElementsLabel.setLabelFor(newElements)
    val layoutPopupTop = new JPanel()
    layoutPopupTop.setOpaque(false)
    layoutPopupTop.add(newElementsLabel)
    layoutPopupTop.add(newElements)

    //LISTENER
    newElements.asInstanceOf[{def addActionListener(l:ActionListener)}].addActionListener(new ActionListener() {
      override def actionPerformed (actionEvent: ActionEvent) {
        val uiElems = UIHelper.getSelectedItem(newElements) match {
          case label if (label == portLabel) => {
            createPortPanel(newPopup, kernel, componentType)
          }
          case label if (label == dictionaryLabel) => {
            createDictionaryPanel(newPopup, kernel, componentType)
          }
          //case _@e => throw new UnsupportedOperationException("No popup implemented for item:" + e); null
        }
        if (uiElems != null) {
          layoutPopup.removeAll()
          layoutPopup.add(uiElems._1, BorderLayout.CENTER)
          layoutPopup.add(uiElems._2, BorderLayout.SOUTH)
          layoutPopup.add(layoutPopupTop, BorderLayout.NORTH)
          layoutPopup.repaint()
          layoutPopup.revalidate()
        }

      }
    })

    layoutPopup.add(layoutPopupTop, BorderLayout.NORTH)
    newPopup.getContentPane.add(layoutPopup)
    val uiElems = if (p != null) {
      p match {
        case label if (label == portLabel) => {
          createPortPanel(newPopup, kernel, componentType)
        }
        case label if (label == dictionaryLabel) => {
          createDictionaryPanel(newPopup, kernel, componentType)
        }
      }
    } else {
      createPortPanel(newPopup, kernel, componentType)
    }
    layoutPopup.add(uiElems._1, BorderLayout.CENTER)
    layoutPopup.add(uiElems._2, BorderLayout.SOUTH)

    newPopup.getJDialog.setVisible(true)
  }

  //val currentProps = new Properties


}