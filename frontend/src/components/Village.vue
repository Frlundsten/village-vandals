<template>
  <div class="bg-[url(../assets/jpeg/isomap.png)] relative h-screen rounded-xl bg-center bg-no-repeat bg-cover bg-fixed">
    <div class="w-[22rem] h-[22rem] absolute left-1/2 top-1/3 transform -translate-x-1/2 pt-[2.25%]">
      <img :src="buildingImage" alt="Center Building" class="w-auto h-auto" />

    </div>
  </div>
</template>
<script setup>
import buildingImage from '@/assets/jpeg/cleancenterbuilding.png';
</script>