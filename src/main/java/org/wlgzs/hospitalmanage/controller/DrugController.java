package org.wlgzs.hospitalmanage.controller;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.wlgzs.hospitalmanage.base.BaseController;
import org.wlgzs.hospitalmanage.entity.Drug;
import org.wlgzs.hospitalmanage.util.Result;
import org.wlgzs.hospitalmanage.util.ResultCode;

/**
 * @author 武凯焱
 * @date 2018/8/14 21:24
 */

@RestController
@RequestMapping("drug")
public class DrugController extends BaseController {
    @GetMapping("drug/{page}")
    // 查询药物
    public ModelAndView getDrugs(Model model, @PathVariable("page") int page) {
        model.addAttribute("drugs", drugService.getDrugs(model,page));
        return new ModelAndView();
    }
    //进入添加药物页面
    @GetMapping("toAdd")
    public ModelAndView toAdd(Model model){
    model.addAttribute("drugType",drugAttributeService.getAttribute(1)); //药品类型
    model.addAttribute("drugUnit",drugAttributeService.getAttribute(2));  //药品单位
    model.addAttribute("dosageForm",drugAttributeService.getAttribute(3)); //药品剂型
        return new ModelAndView();
    }
    // 添加药物
    @PostMapping("drug")
    public Result addDrugs(Drug drug) {
        System.out.println(drug);
        boolean isAdd = drugService.addDrug(drug);
        if (isAdd) {
            return new Result(ResultCode.SUCCESS, "添加成功");
        } else {
            return new Result(ResultCode.FAIL, "药品名字重复");

        }
    }

    // 更新药物信息
    @PutMapping("drug")
    public Result updateDrug(Drug drug) {
        drugService.updateDrug(drug);
        return new Result(ResultCode.SUCCESS, "更改成功");
    }
    //删除药物
    @DeleteMapping("drug/{drugCode}")
    public Result deleteDrug(@PathVariable("drugCode") int drugCode) {
        drugService.deleteDrug(drugCode);
        return new Result(ResultCode.SUCCESS, "删除成功");
    }

    //搜索药物下拉框提示
    @PostMapping("/keyword")
    public Result keyword(@RequestParam("drugName") String drugName) {
        return new Result(ResultCode.SUCCESS, drugService.keyword(drugName));
    }

    //搜索药物
    @GetMapping("/searchdrug/{page}")
    public ModelAndView searchDrug(Model model, @RequestParam("drugName") String drugName,@PathVariable("page") int page ) {
        model.addAttribute("drugs", drugService.searchDrug(model,drugName ,page));
        return new ModelAndView();
    }
}