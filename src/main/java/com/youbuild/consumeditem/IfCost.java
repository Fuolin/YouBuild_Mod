package com.youbuild.consumeditem;

public class IfCost {
     public static class ifcost {
          private boolean ifcostcardiac = false;
          private boolean ifcostitem = false;

         public boolean getIfCostCardiac() {
               return ifcostcardiac;
          }

          public boolean getIfCostItem() {
              return ifcostitem;
          }

          public void setIfCost(boolean setitem,boolean setcardiac) {
              this.ifcostcardiac=setcardiac;
              this.ifcostitem=setitem;
          }
     }
}
