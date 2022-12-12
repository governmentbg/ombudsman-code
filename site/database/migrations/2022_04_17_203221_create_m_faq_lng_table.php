<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

class CreateMFaqLngTable extends Migration
{
    /**
     * Run the migrations.
     *
     * @return void
     */
    public function up()
    {
        Schema::create('m_faq_lng', function (Blueprint $table) {
            $table->increments('FqL_id');
            $table->integer('Fq_id')->comment('FAQ ID')->unsigned();
            $table->integer('S_Lng_id')->comment('Lng ID')->unsigned();
            $table->string('FqL_path', 90)->index()->nullable();
            $table->string('FqL_title', 400)->nullable();

            $table->text('FqL_body')->nullable();
            $table->string('FqL_meta', 300)->nullable();

            $table->timestamps();
            $table->softDeletes();

            $table->foreign('Fq_id')->references('Fq_id')->on('m_faq');
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
        Schema::dropIfExists('m_faq_lng');
    }
}
