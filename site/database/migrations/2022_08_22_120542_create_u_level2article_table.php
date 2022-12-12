<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

class CreateULevel2articleTable extends Migration
{
    /**
     * Run the migrations.
     *
     * @return void
     */
    public function up()
    {
        Schema::create('u_level2article', function (Blueprint $table) {
            $table->increments('Ula_id');
            $table->integer('Ul_id')->nullable()->unsigned();
            $table->integer('Ar_id')->nullable()->unsigned();





            $table->timestamps();
            $table->softDeletes();

            $table->foreign('Ul_id')->references('Ul_id')->on('u_levels');
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
        Schema::dropIfExists('u_level2article');
    }
}
