<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

class CreateMObmudsmanLngTable extends Migration
{
    /**
     * Run the migrations.
     *
     * @return void
     */
    public function up()
    {
        Schema::create('m_ombudsman_lng', function (Blueprint $table) {
            $table->increments('OmbL_id');
            $table->integer('Omb_id')->comment('Ombudsman ID')->unsigned();
            $table->integer('S_Lng_id')->comment('Lng ID')->unsigned();
            $table->string('OmbL_title', 255)->nullable();
            $table->text('OmbL_intro')->nullable();
            $table->text('OmbL_body')->nullable();


            $table->timestamps();
            $table->softDeletes();

            $table->foreign('Omb_id')->references('Omb_id')->on('m_ombudsman');
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
        Schema::dropIfExists('m_ombudsman_lng');
    }
}
