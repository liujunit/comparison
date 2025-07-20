function openTab(evt, tabId) {
    // 隐藏所有标签内容
    const tabContents = document.getElementsByClassName("tab-content");
    for (let i = 0; i < tabContents.length; i++) {
        tabContents[i].classList.remove("active");
    }

    // 移除所有按钮的 active 类
    const tabButtons = document.getElementsByClassName("tab-button");
    for (let i = 0; i < tabButtons.length; i++) {
        tabButtons[i].classList.remove("active");
    }

    // 显示当前标签内容并激活按钮
    document.getElementById(tabId).classList.add("active");
    evt.currentTarget.classList.add("active");
}

// 当有差异时显示修复按钮
document.addEventListener('DOMContentLoaded', function() {
    // 一键修复按钮点击事件
    document.getElementById('repairBtn').addEventListener('click', function() {
        const progressContainer = document.getElementById('progressContainer');
        const progressBar = document.getElementById('repairProgress');
        const repairResults = document.getElementById('repairResults');

        // 显示div
        document.getElementById('repairResults').style.display = 'block';
        document.getElementById('progressContainer').style.display = 'block';

        // 显示进度条
        progressContainer.style.display = 'block';
        repairResults.style.display = 'none';

        $('#resultDataSection').html('');

        // 模拟进度更新
        let progress = 0;
        const interval = setInterval(function() {
            progress += 5;
            progressBar.style.width = progress + '%';
            progressBar.setAttribute('aria-valuenow', progress);

            if (progress >= 100) {
                clearInterval(interval);
                // 隐藏进度条，显示结果
                progressContainer.style.display = 'none';
                repairResults.style.display = 'block';

                // 填充修复结果表格
                populateRepairResults();
            }
        }, 2000); // 总共10秒完成
    });
});

// 填充修复结果表格
function populateRepairResults() {
    const results = [
        { table: 'tb_user', operation: '同步缺失数据', status: '成功', rows: 7, time: '1.2s' },
        { table: 'sys_captcha', operation: '同步缺失数据', status: '成功', rows: 13, time: '1.2s' },
        { table: 'sys_log', operation: '同步主键丢失', status: '成功', rows: 1, time: '5.5s' },
        { table: 'qrtz_blob_triggers', operation: '同步主键丢失', status: '成功', rows: 1, time: '3.5s' },
        { table: 'qrtz_blob_triggers', operation: '同步缺失数据', status: '成功', rows: 120, time: '7.8s' },
        { table: 'schedule_job_log', operation: '同步缺失数据', status: '成功', rows: 10, time: '1.5s' },
        { table: 'qrtz_calendqrtz_calendars', operation: '同步缺失数据', status: '成功', rows: 45, time: '2.1s' },
        { table: 'sys_role_menu', operation: '创建缺失表', status: '成功', rows: 25, time: '2.8s' },
        { table: 'sys_oss', operation: '创建缺失表', status: '成功', rows: 18, time: '1.8s' }
    ];

    const tbody = document.getElementById('repairResultsBody');
    tbody.innerHTML = '';

    results.forEach(result => {
        const row = document.createElement('tr');
        row.className = 'table-success'; // 假设所有修复都成功

        row.innerHTML = `
                <td style="vertical-align: middle;">${result.table}</td>
                <td style="vertical-align: middle;">${result.operation}</td>
                <td style="vertical-align: middle;"><span class="badge bg-success">${result.status}</span></td>
                <td style="vertical-align: middle;">${result.rows}</td>
                <td style="vertical-align: middle;">${result.time}</td>
            `;

        tbody.appendChild(row);
    });
}

$(document).ready(function () {

    // 隐藏div
    document.getElementById('repairResults').style.display = 'none';
    document.getElementById('progressContainer').style.display = 'none';

    $('#dataSourceForm').on('submit', function (e) {
        // 隐藏div
        document.getElementById('repairResults').style.display = 'none';
        document.getElementById('progressContainer').style.display = 'none';

        e.preventDefault();

        // 显示加载状态（保持卡片结构）
        $('#resultDataSection').html(`
        <div class="card">
            <div class="card-header">
                <h5 class="mb-0">数据库对比结果</h5>
            </div>
            <div class="card-body text-center py-5">
                <div class="spinner-border text-primary" style="width: 3rem; height: 3rem;" role="status">
                    <span class="visually-hidden">正在对比数据库...</span>
                </div>
                <p class="mt-3 mb-0">正在对比数据库，请稍候...</p>
            </div>
        </div>
    `);

        $.ajax({
            type: 'POST',
            url: $(this).attr('action'),
            data: $(this).serialize(),
            success: function (response) {

                setTimeout(() => {
                    $('#resultDataSection').html(
                        $(response).find('#resultDataSection').html()
                    );

                    // 动态显示/隐藏修复按钮
                    const hasDifferences = $(response).find('tr.table-danger').length > 0;
                    document.getElementById('repairBtn').style.display = hasDifferences ? 'block' : 'none';
                }, 7000);

            },
            error: function () {
                // 恢复原始内容并显示错误
                $('#resultDataSection').html(`
                    <div class="card">
                        <div class="card-header">
                            <h5 class="mb-0">数据库对比结果</h5>
                        </div>
                        <div class="card-body">
                            <div class="alert alert-danger">
                                <i class="bi bi-exclamation-triangle-fill"></i>
                                对比失败，请重试或检查服务器连接
                            </div>
    
                        </div>
                    </div>
            `   );
                // 出错时隐藏修复按钮
                document.getElementById('repairBtn').style.display = 'none';
            }
        });
    });


    $('#fileSourceForm').on('submit', function (e) {
        e.preventDefault();

        // 显示加载状态（保持卡片结构）
        $('#resultFileSection').html(`
        <div class="card">
            <div class="card-header">
                <h5 class="mb-0">代码对比结果</h5>
            </div>
            <div class="card-body text-center py-5">
                <div class="spinner-border text-primary" style="width: 3rem; height: 3rem;" role="status">
                    <span class="visually-hidden">正在对比文件...</span>
                </div>
                <p class="mt-3 mb-0">正在对比文件，请稍候...</p>
            </div>
        </div>
    `);

        $.ajax({
            type: 'POST',
            url: $(this).attr('action'),
            data: $(this).serialize(),
            success: function (response) {
                $('#resultFileSection').html(
                    $(response).find('#resultFileSection').html()
                );
            },
            error: function () {
                // 恢复原始内容并显示错误
                $('#resultFileSection').html(`
                <div class="card">
                    <div class="card-header">
                        <h5 class="mb-0">代码对比结果</h5>
                    </div>
                    <div class="card-body">
                        <div class="alert alert-danger">
                            <i class="bi bi-exclamation-triangle-fill"></i>
                            对比失败，请重试或检查服务器连接
                        </div>

                    </div>
                </div>
            `);
            }
        });
    });
});