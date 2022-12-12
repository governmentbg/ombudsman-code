<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

class CreateMgalLngTable extends Migration
{
    /**
     * Run the migrations.
     *
     * @return void
     */
    public function up()
    {
        Schema::create('m_gallery_lng', function (Blueprint $table) {
            $table->increments('ArGL_id');
            $table->integer('ArG_id')->comment('Gallery ID')->unsigned();


            $table->string('ArGL_name', 250)->nullable();




            $table->timestamps();
            $table->softDeletes();

            $table->foreign('ArG_id')->references('ArG_id')->on('m_gallery');
        });
    }

    /**
     * Reverse the migrations.
     *
     * @return void
     */
    public function down()
    {
        Schema::dropIfExists('m_gallery_lng');
    }
}
