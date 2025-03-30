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


$(document).ready(function () {
    $('#dataSourceForm').on('submit', function (e) {
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
                $('#resultDataSection').html(
                    $(response).find('#resultDataSection').html()
                );
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
            `);
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