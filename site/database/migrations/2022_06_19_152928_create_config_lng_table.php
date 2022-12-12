<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

class CreateConfigLngTable extends Migration
{
    /**
     * Run the migrations.
     *
     * @return void
     */
    public function up()
    {
        Schema::create('m_config_lng', function (Blueprint $table) {
            $table->increments('CfL_id');
            $table->integer('Cf_id')->comment('Config ID')->unsigned()->nullable();
            $table->integer('S_Lng_id')->comment('Lng ID')->unsigned()->nullable();
            $table->string('CfL_name', 200)->nullable();
            $table->string('CfL_value', 1000)->nullable();



            $table->timestamps();
            $table->softDeletes();

            $table->foreign('S_Lng_id')->references('S_Lng_id')->on('s_lang');
            $table->foreign('Cf_id')->references('Cf_id')->on('m_config');
        });
    }

    /**
     * Reverse the migrations.
     *
     * @return void
     */
    public function down()
    {
        Schema::dropIfExists('m_config_lng');
    }
}
