<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

class AddTypeToConfigTable extends Migration
{
    /**
     * Run the migrations.
     *
     * @return void
     */
    public function up()
    {
        Schema::table('m_config', function (Blueprint $table) {
            $table->integer('CfT_id')->comment('Type configuration')->after('S_Lng_id')->nullable()->unsigned();

            $table->foreign('CfT_id')->references('CfT_id')->on('m_config_type');
        });
    }

    /**
     * Reverse the migrations.
     *
     * @return void
     */
    public function down()
    {
        Schema::table('m_config', function (Blueprint $table) {
            //
        });
    }
}
