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
package org.kevoree.api.service.core.handler;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

import org.kevoree.ContainerRoot;
import org.kevoree.context.ContextRoot;

import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * @author ffouquet
 */
public interface KevoreeModelHandlerService {

    public ContainerRoot getLastModel();

    public UUIDModel getLastUUIDModel();

    public Date getLastModification();

    public void updateModel(ContainerRoot model);

    public void updateModel(ContainerRoot model, ModelUpdateCallback callback);

    public Date atomicUpdateModel(ContainerRoot model);

    public void compareAndSwapModel(UUIDModel previousModel, ContainerRoot targetModel);

    public void compareAndSwapModel(UUIDModel previousModel, ContainerRoot targetModel, ModelUpdateCallback callback);

    public Date atomicCompareAndSwapModel(UUIDModel previousModel, ContainerRoot targetModel) throws KevoreeModelUpdateException;

    public List<ContainerRoot> getPreviousModel();

    public String getNodeName();

    public void registerModelListener(ModelListener listener);

    public void unregisterModelListener(ModelListener listener);

    public ContextRoot getContextModel();

    public void acquireLock(ModelHandlerLockCallBack callBack, Long timeout);

    public void releaseLock(UUID uuid);

    public boolean checkModel(ContainerRoot targetModel);

}
