package org.wlgzs.hospitalmanage.service.serviceImpl;

import com.github.pagehelper.PageHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestParam;
import org.wlgzs.hospitalmanage.dao.DrugInventoryMapper;
import org.wlgzs.hospitalmanage.dao.DrugMapper;
import org.wlgzs.hospitalmanage.entity.Drug;
import org.wlgzs.hospitalmanage.entity.DrugInventory;
import org.wlgzs.hospitalmanage.entity.StorageRecord;
import org.wlgzs.hospitalmanage.service.DrugInventoryService;
import org.wlgzs.hospitalmanage.service.StorageRecordService;
import org.wlgzs.hospitalmanage.util.Result;
import org.wlgzs.hospitalmanage.util.ResultCode;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author 武凯焱
 * @date 2018/8/19 11:14
 */
@Service
public class DrugInventoryServiceImpl implements DrugInventoryService {
    @Resource
    DrugInventoryMapper drugInventoryMapper;
    @Autowired
    HttpSession session;
    @Autowired
    StorageRecordService storageRecordService;
    @Resource
    DrugMapper drugMapper;

    //添加药品库存
    public Result addDrugInventory(DrugInventory drugInventory,String dateStr) {
        boolean isAdd;
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        try {
            Date date = df.parse(dateStr);
            drugInventory.setValid_period(date);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        try {
            drugInventoryMapper.insert(drugInventory);
            isAdd = true;
        } catch (Exception e) {
            isAdd = false;
        }
        if (isAdd) {
            BigDecimal storageAmount = drugInventory.getStorage_amount();
            int drugCode = drugInventory.getDrug_code();
            Date validPeriodDate = drugInventory.getValid_period();
            Date currentDate = new Date();
            /*if (session.getAttribute("user") == null) {
                return new Result(ResultCode.FAIL, "请先登录");
            }*/
        //    int operator_code = (int) session.getAttribute("user");
            if (storageAmount.compareTo(new BigDecimal("0")) <= 0 && validPeriodDate == null) {
                return new Result(ResultCode.FAIL, "请将信息填写正确");
            }
            StorageRecord storageRecord = new StorageRecord(drugCode, 001, storageAmount.intValue(), currentDate, validPeriodDate);
            storageRecordService.record(storageRecord);
            DrugInventory currentDrugInventory = drugInventoryMapper.increase(drugCode);
            BigDecimal receipt = drugInventory.getStorage_amount();
            BigDecimal currentReceipt = currentDrugInventory.getStorage_amount().add(receipt);
            Drug drug = drugMapper.selectByPrimaryKey(drugCode);
            BigDecimal safeStorage = drug.getSafety_stock();                //药品安全库存
            if (currentReceipt.compareTo(safeStorage)<0){
                currentDrugInventory.setIs_safety_stock(0);
            }else {
                System.out.println(currentReceipt);
                System.out.println(safeStorage);
                currentDrugInventory.setIs_safety_stock(1);
            }
            currentDrugInventory.setStorage_amount(currentReceipt);
            System.out.println("dfd"+currentDrugInventory);
            drugInventoryMapper.updatenIventory(currentDrugInventory);   //更新库存数量

            return new Result(ResultCode.SUCCESS, "添加成功");

        } else {
            return new Result(ResultCode.FAIL, "添加失败，药品编码错误");
        }
    }
    //删除库存
    public void deleteDrugInventory(@RequestParam("drugInventory") int drugInventory) {
        drugInventoryMapper.deleteInventory(drugInventory);
    }
    //更改库存
    public boolean updateDrugInventory(DrugInventory drugInventory) {
        return true;
    }

    public boolean reduceInventories(int drugCode, String num) {
        BigDecimal drugAmount = new BigDecimal("num"); //处方要减数量
        DrugInventory drugInventory = drugInventoryMapper.increase(drugCode); //总库存量
        List<DrugInventory> drugInventories = drugInventoryMapper.reduceInventories(drugCode); //按有效期的分库存量
        if (drugInventory.getStorage_amount().compareTo(drugAmount) < 0) {
            return false;
        }
        Drug drug = drugMapper.selectByPrimaryKey(drugCode);
        BigDecimal safeStorage = drug.getSafety_stock();                //药品安全库存
        BigDecimal currentStorage = drugInventory.getStorage_amount().subtract(drugAmount);//剩余库存量
        if (currentStorage.compareTo(safeStorage)<0){
            drugInventory.setIs_safety_stock(0);
        }else {
            drugInventory.setIs_safety_stock(1);
        }
        drugInventory.setStorage_amount(currentStorage);
        drugInventoryMapper.updateByPrimaryKey(drugInventory); //将减过的总数库存对象存到数据库
        for (DrugInventory Inventory : drugInventories) {
            BigDecimal temp = Inventory.getStorage_amount();
            if (temp.compareTo(drugAmount) >= 0) {
                BigDecimal currentAmount = temp.subtract(drugAmount);
                Inventory.setStorage_amount(currentAmount);
                drugInventoryMapper.updateByPrimaryKey(Inventory);
                break;
            } else {
                drugAmount = drugAmount.subtract(temp);
                Inventory.setStorage_amount(new BigDecimal(0));
                drugInventoryMapper.updateByPrimaryKey(Inventory);
            }
        }
        return true;
    }

    public List<DrugInventory> getDrugInventory(int page) {
        return drugInventoryMapper.getAll();
    }
    //获得有效期一个月的药品
    public List<DrugInventory> getMonthLimit(){
        List<DrugInventory> drugInventoryList = drugInventoryMapper.getAllnotnull();
        List<DrugInventory> monthLimit = new ArrayList<DrugInventory>();
        long currentTime = System.currentTimeMillis();
        for (DrugInventory drugInventory :drugInventoryList) {
            long valid_period = drugInventory.getValid_period().getTime();
            long result = valid_period=currentTime;
            if (result<=2678400L){
                monthLimit.add(drugInventory);
            }
        }
         return monthLimit;
    }
    //获取低于安全库存的药品清单
    public List<DrugInventory> getUnsafetyStock(Model model,int page){
      //  List<DrugInventory> drugInventoryList = drugInventoryMapper.getAll();
        PageHelper.startPage(page,10);
        List<DrugInventory> unSafetyStock = drugInventoryMapper.getUnsafetyStock();
        int count = drugInventoryMapper.getUnsafeCount();
        model.addAttribute("pages",Math.ceil(count/10.0));
        model.addAttribute("page",page);

      /*  for (DrugInventory drugInventory:drugInventoryList
             ) {
           int drug_code =  drugInventory.getDrug_code();
           BigDecimal  storage_amount = drugInventory.getStorage_amount();
           Drug drug = drugMapper.selectByPrimaryKey(drug_code);
           BigDecimal  safe_storage = drug.getSafety_stock();
           if (safe_storage.compareTo(storage_amount)>0){
              unSafetyStock.add(drugInventory);
           }
        }*/
         return unSafetyStock;
    }
    public void updateDrugInventory(DrugInventory drugInventory,String dateStr){
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        try {
            Date date = df.parse(dateStr);
            drugInventory.setValid_period(date);
        } catch (ParseException e) {
            e.printStackTrace();
        }
         BigDecimal currentAmount =  drugInventory.getStorage_amount();
         Drug drug = drugMapper.selectByPrimaryKey(drugInventory.getDrug_code());
         BigDecimal safeAmount = drug.getSafety_stock();
         if (currentAmount.compareTo(safeAmount)<=0){
             drugInventory.setIs_safety_stock(0);
         }else {
             drugInventory.setIs_safety_stock(1);
         }
         drugInventoryMapper.update(drugInventory);
    }
    public List<DrugInventory>  searchStorage(Model model, String drugName,int page){
        PageHelper.startPage(page, 10);
         List<DrugInventory> drugInventories = drugInventoryMapper.searchStroage(drugName);
         int pages =(int)Math.ceil(drugInventories.size()/10.0);
         model.addAttribute("pages",pages);
         model.addAttribute("page",page);
         return drugInventories;
    }
    public List<DrugInventory> keyword(String drugName){
        List<DrugInventory> drugInventories = drugInventoryMapper.keyword(drugName);
        return drugInventories;
    }
}