/*
Copyright 2020 SkillTree

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
<template>
  <!-- This footer will appear at the top of every page, update this component to customize for your app -->
  <div v-html="customFooter">
  </div>
</template>

<script>
  import DynamicVariableReplacementMixin from './DynamicVariableReplacementMixin';

  export default {
    name: 'CustomizableFooter',
    mixins: [DynamicVariableReplacementMixin],
    computed: {
      customFooter() {
        return this.populateDynamicVariables(this.$store.getters.config.customFooter);
      },
    },
  };
</script>

<style scoped>

</style>
