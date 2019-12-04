import React, {
  useCallback, useContext, useEffect,
} from 'react';
import { toJS } from 'mobx';
import { observer } from 'mobx-react-lite';
import { withRouter } from 'react-router-dom';
import _ from 'lodash';
import { FormattedMessage } from 'react-intl';
import {
  Page, Header, Content, Breadcrumb, Choerodon,
} from '@choerodon/boot';
import {
  Icon, Tabs, Spin,
} from 'choerodon-ui';
import { Modal, Button } from 'choerodon-ui/pro';
import { editExecuteDetail } from '../../../api/cycleApi';
import { deleteExecute, quickPassOrFail } from '../../../api/TestPlanApi';
import CreateAutoTest from '../components/CreateAutoTest';
import TestPlanDetailCard from '../components/TestPlanDetailCard';
import TestPlanStatusCard from '../components/TestPlanStatusCard';
import UpdateRemindModalChildren from '../components/UpdateRemindModalChildren';
import TestPlanTree from '../components/TestPlanTree';
import TestPlanTable from '../components/TestPlanTable';
import TestPlanHeader from '../components/TestPlanHeader';
import { openCreatePlan } from '../components/TestPlanModal';
import Empty from '../../../components/Empty';
import testCaseEmpty from './testCaseEmpty.svg';

import Store from '../stores';
import './TestPlanHome.less';
import { getDragRank, executeDetailLink } from '../../../common/utils';
import Item from 'choerodon-ui/lib/list/Item';


const { TabPane } = Tabs;
const { confirm } = Modal;
const updateRemindModal = Modal.key();

function TestPlanHome() {
  const {
    prefixCls, createAutoTestStore, testPlanStore, history,
  } = useContext(Store);
  const {
    treeData, loading, checkIdMap, testList, testPlanStatus, planInfo, statusList,
  } = testPlanStore;

  const handleTabsChange = (value) => {
    // testPlanStore.clearStore();
    testPlanStore.setTestPlanStatus(value);
    testPlanStore.setCurrentCycle({});
    testPlanStore.loadAllData();
  };

  const handleUpdateOk = () => {

  };

  const handleIgnoreUpdate = () => {

  };
  const handleOpenCreatePlan = () => {
    openCreatePlan({
      onCreate: () => {
        if (testPlanStatus !== 'todo') {
          testPlanStore.setTestPlanStatus('todo');
        }      
        testPlanStore.loadAllData();
      },
    });
  };

  const handleOpenUpdateRemind = (e) => {
    e.stopPropagation();
    Modal.open({
      key: updateRemindModal,
      drawer: true,
      title: '用例变更提醒',
      children: <UpdateRemindModalChildren testPlanStore={testPlanStore} />,
      style: { width: '10.9rem' },
      className: 'c7ntest-testPlan-updateRemind-modal',
      okText: '更新',
      cancelText: '取消',
      onOk: handleUpdateOk,
      footer: (okBtn, cancelBtn) => (
        <div>
          {okBtn}
          <Button funcType="funcType" onClick={handleIgnoreUpdate}>忽略更新</Button>
          {cancelBtn}
        </div>
      ),
    });
  };

  const handleTableSummaryClick = (record) => {
    history.push(executeDetailLink(record.executeId));
  };

  const onDragEnd = (sourceIndex, targetIndex) => {
    const { lastRank, nextRank } = getDragRank(sourceIndex, targetIndex, testList);
    const source = testList[sourceIndex];
    const temp = { ...source };
    delete temp.defects;
    delete temp.caseAttachment;
    delete temp.testCycleCaseStepES;
    delete temp.issueInfosVO;
    temp.assignedTo = temp.assignedTo || 0;
    testPlanStore.setTableLoading(true);
    editExecuteDetail({
      ...temp,
      ...{
        lastRank,
        nextRank,
      },
    }).then((res) => {
      testPlanStore.loadExecutes();
    }).catch((err) => {
      Choerodon.prompt('网络错误');
      testPlanStore.setTableLoading(false);
    });
  };

  const handleExecuteTableChange = (pagination, filters, sorter, barFilters) => {
    let { filter } = testPlanStore;
    Object.keys(filters).map((key) => {
      if (filters[key] && filters[key].length > 0) {
        filter = { ...filter, [key]: filters[key][0] };
      } else {
        filter[key] = '';
      }
    });    
    testPlanStore.setBarFilter(barFilters || []);
    if (pagination.current) {
      testPlanStore.setFilter(filter);
      testPlanStore.setExecutePagination(pagination);
      testPlanStore.loadExecutes();
    }
  };

  const handleDeleteExecute = (record) => {
    const { executeId } = record;
    confirm({
      width: 560,
      title: Choerodon.getMessage('确认删除吗?', 'Confirm delete'),
      content: Choerodon.getMessage('当您点击删除后，该条执行将从此计划阶段中移除!', 'When you click delete, after which the data will be deleted !'),
      onOk: () => {
        deleteExecute(executeId)
          .then(() => {
            testPlanStore.loadExecutes();
          }).catch((err) => {
            /* console.log(err); */
            Choerodon.prompt('删除失败');
          });
      },
      okText: '删除',
      okType: 'danger',
    });
  };

  const handleQuickPassOrFail = (execute, isPass = true, e) => {
    e.stopPropagation();
    let executionStatus;
    if (isPass) {
      const { statusId } = statusList.find(status => status.statusName === '通过') || {};
      executionStatus = statusId;
    } else {
      const { statusId } = statusList.find(status => status.statusName === '失败') || {};
      executionStatus = statusId;
    }
    const data = {
      executionStatus,
      executeId: execute.executeId,
      objectVersionNumber: execute.objectVersionNumber,
    };
    quickPassOrFail(data).then(() => {
      testPlanStore.loadExecutes();
    }).catch(() => {
      if (isPass) {
        Choerodon.prompt('快速通过失败');
      } else {
        Choerodon.prompt('快速失败失败');
      }
    });
  };

  const handleAssignToChange = (value) => {
    if (value && checkIdMap.size) {
      testPlanStore.executesAssignTo(value).then(() => {
        checkIdMap.clear();
      });
    }
  };

  useEffect(() => {
    testPlanStore.loadAllData();
  }, [testPlanStore]);

  const noPlan = treeData.rootIds && treeData.rootIds.length === 0;
  let description;
  if (testPlanStatus === 'todo') {
    description = '当前项目下无未开始的计划';
  } else if (testPlanStatus === 'doing') {
    description = '当前项目下无进行中的计划';
  } else if (testPlanStatus === 'done') {
    description = '当前项目下无已完成的计划';
  }
  return (
    <Page className={prefixCls}>
      <Header
        title={<FormattedMessage id="testPlan_name" />}
      >
        <Button icon="playlist_add icon" onClick={handleOpenCreatePlan}>
          <FormattedMessage id="testPlan_createPlan" />
        </Button>
        <TestPlanHeader />
      </Header>
      <Breadcrumb />
      <Content style={{ display: 'flex', padding: '0', borderTop: '0.01rem solid rgba(0,0,0,0.12)' }}>
        <div className={`${prefixCls}-contentWrap`}>
          <div className={`${prefixCls}-contentWrap-left`}>
            <div className={`${prefixCls}-contentWrap-testPlanTree`}>
              <Tabs defaultActiveKey="todo" onChange={handleTabsChange} value={testPlanStatus}>
                <TabPane tab="未开始" key="todo">
                  <TestPlanTree />
                </TabPane>
                <TabPane tab="进行中" key="doing">
                  <TestPlanTree />
                </TabPane>
                <TabPane tab="已完成" key="done">
                  <TestPlanTree />
                </TabPane>
              </Tabs>
            </div>
          </div>
          {
          noPlan ? (
            <Empty
              loading={loading}
              pic={testCaseEmpty}
              title="暂无计划"
              description={description}
              // extra={<Button color="primary" funcType="raised" onClick={handleOpenCreatePlan}>创建计划</Button>}
            />
          ) : (
            <div className={`${prefixCls}-contentWrap-right`}>
              <div className={`${prefixCls}-contentWrap-right-currentPlanName`}>
                <Icon type="insert_invitation" />
                <span>{planInfo.name}</span>
              </div>
              <div className={`${prefixCls}-contentWrap-right-warning`}>
                <Icon type="error" />
                <span>该计划正在进行自动化测试，手工测试结果可能会将自动化测试结果覆盖！</span>
              </div>
              <div className={`${prefixCls}-contentWrap-right-card`}>
                <div style={{ display: 'flex', justifyContent: 'space-between', flexWrap: 'nowrap' }}>
                  <div style={{ flex: 1, marginRight: '0.16rem' }}>
                    <TestPlanDetailCard />
                  </div>
                  <div style={{ flex: 1, overflowX: 'hidden' }}>
                    <TestPlanStatusCard />
                  </div>
                </div>
                <div className={`${prefixCls}-contentWrap-table`}>
                  <TestPlanTable
                    onDragEnd={onDragEnd}
                    onTableChange={handleExecuteTableChange}
                    onDeleteExecute={handleDeleteExecute}
                    onQuickPass={handleQuickPassOrFail}
                    onQuickFail={handleQuickPassOrFail}
                    onAssignToChange={handleAssignToChange}
                    onOpenUpdateRemind={handleOpenUpdateRemind}
                    onTableSummaryClick={handleTableSummaryClick}
                  />
                </div>
              </div>
            </div>
          )
        }
        </div>
      </Content>
      <CreateAutoTest createAutoTestStore={createAutoTestStore} />
    </Page>
  );
}

export default withRouter(observer(TestPlanHome));
