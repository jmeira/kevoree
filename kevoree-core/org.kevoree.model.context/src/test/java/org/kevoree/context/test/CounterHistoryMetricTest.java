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
package org.kevoree.context.test;

import org.junit.Test;
import org.kevoree.context.*;

import java.util.Random;

/**
 * Created with IntelliJ IDEA.
 * User: duke
 * Date: 10/12/12
 * Time: 21:50
 */
public class CounterHistoryMetricTest {

    @Test
    public void CounterTest() {
        ContextRoot model = ContextFactory.createContextRoot();
        Random rand = new Random();

        for (int i = 0; i < 1000 ; i++) {
            Metric cpuMetric = PutHelper.getMetric(model, "perf/cpu/{node42}", PutHelper.getParam().setMetricTypeClazzName(CounterHistoryMetric.class.getName()).setNumber(100));
            PutHelper.addValue(cpuMetric, rand.nextLong() + "");
        }

        Metric m = (Metric) model.findById("perf/cpu/{node42}");
        assert (m.getValuesForJ().size() == 100);

        MetricValue mv = (MetricValue) model.findById("perf/cpu/{node42}/last[]");
        assert (mv != null);
    }


}
