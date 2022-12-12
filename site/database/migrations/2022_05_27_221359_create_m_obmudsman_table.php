<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

class CreateMObmudsmanTable extends Migration
{
    /**
     * Run the migrations.
     *
     * @return void
     */
    public function up()
    {
        Schema::create('m_ombudsman', function (Blueprint $table) {
            $table->increments('Omb_id');

            $table->string('Omb_name', 300)->nullable();
            $table->string('Omb_photo', 300)->nullable();
            $table->date('Omb_date_from')->index()->nullable();
            $table->date('Omb_date_to')->index()->nullable();
            $table->integer('Ar_id')->unsigned()->nullable();

            $table->timestamps();
            $table->softDeletes();

            $table->foreign('Ar_id')->references('Ar_id')->on('m_article');
        });
    }

    /**
     * Reverse the migrations.
     *
     * @return void
     */
    public function down()
    {
        Schema::dropIfExists('m_ombudsman');
    }
}
