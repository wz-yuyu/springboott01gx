<template>
  <div>
    <div
      class="container loginIn"
      style="background-image:url(http://codegen.caihongy.cn/20201230/2de5336c3bc94b8791e8ba4a1b5252cb.jpg)"
    >
      <div class="left center login-card">
        <el-form class="login-form" label-position="left" label-width="0px">
          <div class="title-container">
            <h3 class="title">网上点餐系统登录</h3>
          </div>

          <el-form-item class="style2">
            <span class="svg-container">
              <svg-icon icon-class="user" />
            </span>
            <el-input
              placeholder="请输入用户名"
              v-model="rulesForm.username"
            />
          </el-form-item>

          <el-form-item class="style2">
            <span class="svg-container">
              <svg-icon icon-class="password" />
            </span>
            <el-input
              placeholder="请输入密码"
              type="password"
              v-model="rulesForm.password"
            />
          </el-form-item>

          <el-form-item label="角色" class="role">
            <el-radio
              v-for="item in menus"
              v-if="item.hasBackLogin === '是'"
              :key="item.roleName"
              v-model="rulesForm.role"
              :label="item.roleName"
            >
              {{ item.roleName }}
            </el-radio>
          </el-form-item>

          <el-button class="loginInBt" @click="login">
            登录
          </el-button>

          <el-form-item class="setting">
            <div class="register" @click="register('meishidian')">
              注册美食店
            </div>
          </el-form-item>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script>
import menu from "@/utils/menu";

export default {
  data() {
    return {
      rulesForm: {
        username: "",
        password: "",
        role: ""
      },
      menus: [],
      tableName: ""
    };
  },
  mounted() {
    this.menus = menu.list();
  },
  methods: {
    register(tableName) {
      this.$storage.set("loginTable", tableName);
      this.$router.push({ path: "/register" });
    },
    login() {
      if (!this.rulesForm.username) {
        this.$message.error("请输入用户名");
        return;
      }
      if (!this.rulesForm.password) {
        this.$message.error("请输入密码");
        return;
      }
      if (!this.rulesForm.role) {
        this.$message.error("请选择角色");
        return;
      }

      for (let i of this.menus) {
        if (i.roleName === this.rulesForm.role) {
          this.tableName = i.tableName;
        }
      }

      this.$http({
        url: `${this.tableName}/login`,
        method: "post",
        params: {
          username: this.rulesForm.username,
          password: this.rulesForm.password
        }
      }).then(({ data }) => {
        if (data.code === 0) {
          this.$storage.set("Token", data.token);
          this.$storage.set("role", this.rulesForm.role);
          this.$storage.set("sessionTable", this.tableName);
          this.$storage.set("adminName", this.rulesForm.username);
          this.$router.replace({ path: "/index/" });
        } else {
          this.$message.error(data.msg);
        }
      });
    }
  }
};
</script>

<style lang="scss" scoped>
.loginIn {
  min-height: 100vh;
  background-size: cover;
  background-position: center;
  display: flex;
  align-items: center;
  justify-content: center;

  .login-card {
    width: 360px;
    padding: 32px 24px;
    background: rgba(255, 255, 255, 0.9);
    border-radius: 12px;
    box-shadow: 0 12px 30px rgba(0, 0, 0, 0.2);
  }

  .title-container {
    text-align: center;
    margin-bottom: 28px;

    .title {
      font-size: 22px;
      font-weight: 600;
      color: #333;
    }
  }

  .svg-container {
    position: absolute;
    left: -30px;
    top: 0;
    width: 30px;
    text-align: center;
    line-height: 44px;
    color: #ff7a18;
  }

  :deep(.el-input__inner) {
    height: 44px;
    border-radius: 6px;
    background: #f5f6f7;
    border: 1px solid #e4e7ed;
    color: #333;
  }

  :deep(.el-input__inner:focus) {
    border-color: #ff7a18;
  }

  .loginInBt {
    width: 100%;
    height: 44px;
    margin-top: 10px;
    border-radius: 6px;
    border: none;
    font-size: 16px;
    color: #fff;
    background: linear-gradient(135deg, #ff7a18, #ff9f43);
  }

  .loginInBt:hover {
    opacity: 0.9;
  }

  .role {
    margin-top: 10px;
  }

  :deep(.el-radio__input.is-checked .el-radio__inner) {
    background-color: #ff7a18;
    border-color: #ff7a18;
  }

  :deep(.el-radio__label) {
    color: #666;
  }

  :deep(.el-radio__input.is-checked + .el-radio__label) {
    color: #ff7a18;
  }

  .setting {
    margin-top: 12px;
    text-align: center;

    .register {
      color: #ff7a18;
      cursor: pointer;
    }

    .register:hover {
      text-decoration: underline;
    }
  }
}
</style>
