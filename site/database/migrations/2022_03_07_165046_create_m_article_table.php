<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

class CreateMArticleTable extends Migration
{
    /**
     * Run the migrations.
     *
     * @return void
     */
    public function up()
    {
        Schema::create('m_article', function (Blueprint $table) {
            $table->increments('Ar_id');
            $table->integer('Ar_parent_id')->comment('Parent ID')->unsigned()->nullable();
            $table->string('Ar_name', 255);
            $table->date('Ar_date')->index();

            $table->timestamps();
            $table->softDeletes();

            $table->foreign('Ar_parent_id')->references('Ar_id')->on('m_article');
        });
    }

    /**
     * Reverse the migrations.
     *
     * @return void
     */
    public function down()
    {
        Schema::dropIfExists('m_article');
    }
}
