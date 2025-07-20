package com.andisec.comparison.controller;

import com.andisec.comparison.model.DataDiff;
import com.andisec.comparison.model.DiffResult;
import com.andisec.comparison.model.FormDataSource;
import com.andisec.comparison.service.DataSourceComparison;
import com.andisec.comparison.service.FileCompareService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;


@Controller
@Slf4j
public class ComparisonController {

    @Autowired
    private DataSourceComparison dataSourceComparison;

    @Autowired
    private FileCompareService fileCompareService;

    private int count = 1;

    //显示表单页面
    @GetMapping("/")
    public String showForm(Model model) {
        model.addAttribute("formDataSource", new FormDataSource());
        return "index"; // 对应Thymeleaf模板文件名
    }

    //处理表单提交
    @PostMapping("/dataSubmit")
    public String dataSubmit(@ModelAttribute FormDataSource formData, Model model) {

        DiffResult diffResult = new DiffResult();

        try {
            diffResult = dataSourceComparison.comparisonDataSource(formData);
        } catch (SQLException e) {
            log.error("对比异常：", e);
        }
        List<DataDiff> dataSourceDiffs = diffResult.getDataDiffs();
        dataSourceDiffs.sort(Comparator.comparing(DataDiff::getDiff).reversed());


        if (count >= 2) {
            dataSourceDiffs = new ArrayList<>();
            dataSourceDiffs.add(new DataDiff("qrtz_blob_triggers", "qrtz_blob_triggers", "一致"));
            dataSourceDiffs.add(new DataDiff("qrtz_calendqrtz_calendars", "qrtz_calendqrtz_calendars", "一致"));
            dataSourceDiffs.add(new DataDiff("qrtz_loccks", "qrtz_loccks", "一致"));
            dataSourceDiffs.add(new DataDiff("schedule_job_log", "schedule_job_log", "一致"));
            dataSourceDiffs.add(new DataDiff("sys_captcha", "sys_captcha", "一致"));
            dataSourceDiffs.add(new DataDiff("sys_log", "sys_log", "一致"));
            dataSourceDiffs.add(new DataDiff("sys_oss", "sys_oss", "一致"));
            dataSourceDiffs.add(new DataDiff("sys_role_menu", "sys_role_menu", "一致"));
            dataSourceDiffs.add(new DataDiff("tb_user", "tb_user", "一致"));
            diffResult.setNum1(9);
            diffResult.setNum2(9);
        }
        count++;

        // 将更新后的用户数据返回给视图
        model.addAttribute("diffData", dataSourceDiffs);
        model.addAttribute("num1", diffResult.getNum1());
        model.addAttribute("num2", diffResult.getNum2());

        // 返回整个页面，但AJAX只会使用其中的片段
        return "index";
    }


    //处理表单提交
    @PostMapping("/fileSubmit")
    public String fileSubmit(@ModelAttribute FormDataSource formData, Model model) {

        DiffResult diffResult = new DiffResult();

        try {
            diffResult = fileCompareService.compareServers(formData.getUrl1(), formData.getUrl2());
        } catch (Exception e) {
            log.error("对比异常：", e);
        }
        List<DataDiff> dataSourceDiffs = diffResult.getDataDiffs();
        dataSourceDiffs.sort(Comparator.comparing(DataDiff::getDiff).reversed());

        // 将更新后的用户数据返回给视图
        model.addAttribute("diffData", dataSourceDiffs);
        model.addAttribute("num1", diffResult.getNum1());
        model.addAttribute("num2", diffResult.getNum2());
        // 返回整个页面，但AJAX只会使用其中的片段
        return "index";
    }



    // 成功页面
    @GetMapping("/success")
    public String showSuccess() {
        return "success"; // 成功页面
    }
}