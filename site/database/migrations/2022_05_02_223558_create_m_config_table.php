<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

class CreateMConfigTable extends Migration
{
    /**
     * Run the migrations.
     *
     * @return void
     */
    public function up()
    {
        Schema::create('m_config', function (Blueprint $table) {
            $table->increments('Cf_id');
            $table->integer('S_Lng_id')->comment('Lng ID')->unsigned()->nullable();
            $table->string('Cf_name', 90)->index()->nullable();
            $table->string('Cf_value', 700)->nullable();

            $table->boolean('St_id')->nullable();

            $table->timestamps();
            $table->softDeletes();

            $table->foreign('S_Lng_id')->references('S_Lng_id')->on('s_lang');
        });
    }

    /**
     * Reverse the migrations.
     *
     * @return void
     */
    public function down()
    {
        Schema::dropIfExists('m_config');
    }
}
